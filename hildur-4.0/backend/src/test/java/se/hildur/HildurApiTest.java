package se.hildur;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the whole app (with its in-memory database) and calls the REST API
 * the same way the React app does.
 */
@SpringBootTest(properties = "hildur.auto-assign-delay=300ms")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HildurApiTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ---- login -------------------------------------------------------------

    @Test
    void guestLogsInWithBookingNumberAndLastName() throws Exception {
        postJson("/api/auth/guest", null, "{\"bookingNumber\":\"hx-4821\",\"lastName\":\"lind\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GUEST"))
                .andExpect(jsonPath("$.name").value("Anna L. (12)"));
    }

    @Test
    void wrongLastNameIsRejected() throws Exception {
        postJson("/api/auth/guest", null, "{\"bookingNumber\":\"HX-4821\",\"lastName\":\"Berg\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("errGuest"));
    }

    @Test
    void wrongStaffPinIsRejected() throws Exception {
        postJson("/api/auth/staff", null, "{\"username\":\"lisa.b\",\"pin\":\"1234\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("errStaff"));
    }

    @Test
    void guestTokenCannotOpenStaffApiAndViceVersa() throws Exception {
        String guest = guestToken("HX-4821", "Lind");
        String staff = staffToken();
        mvc.perform(get("/api/staff/dashboard").header("Authorization", "Bearer " + guest))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/guest/me").header("Authorization", "Bearer " + staff))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/guest/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void loggedOutTokenStopsWorking() throws Exception {
        String guest = guestToken("HX-4821", "Lind");
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + guest))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/guest/me").header("Authorization", "Bearer " + guest))
                .andExpect(status().isUnauthorized());
    }

    // ---- guest flow --------------------------------------------------------

    @Test
    void wifiPasswordOnlyAfterCheckIn() throws Exception {
        String guest = guestToken("HX-4821", "Lind");
        mvc.perform(get("/api/guest/wifi").header("Authorization", "Bearer " + guest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("checkInFirst"));
        postJson("/api/guest/check-in", guest, "{}").andExpect(jsonPath("$.checkedIn").value(true));
        mvc.perform(get("/api/guest/wifi").header("Authorization", "Bearer " + guest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("Norrsken-Kjell-12"));
    }

    @Test
    void bookingSaunaTakesASeatAndMovingGivesItBack() throws Exception {
        String guest = guestToken("HX-4821", "Lind");
        long six = slotId(guest, "06:00");
        long eight = slotId(guest, "08:00");

        postJson("/api/guest/sauna/booking", guest, "{\"slotId\":" + six + "}")
                .andExpect(jsonPath("$.saunaTime").value("06:00"));
        assertBooked(guest, "06:00", 6);

        postJson("/api/guest/sauna/booking", guest, "{\"slotId\":" + eight + "}");
        assertBooked(guest, "06:00", 5);
        assertBooked(guest, "08:00", 3);

        mvc.perform(delete("/api/guest/sauna/booking").header("Authorization", "Bearer " + guest))
                .andExpect(jsonPath("$.saunaTime").doesNotExist());
        assertBooked(guest, "08:00", 2);
    }

    @Test
    void fullSaunaSlotCannotBeBooked() throws Exception {
        String guest = guestToken("HX-4821", "Lind");
        postJson("/api/guest/sauna/booking", guest, "{\"slotId\":" + slotId(guest, "07:00") + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("slotFull"));
    }

    @Test
    void breakfastOrderIsSaved() throws Exception {
        String guest = guestToken("HX-5530", "Berg");
        putJson("/api/guest/breakfast", guest, "{\"items\":[\"COFFEE\",\"WAFFLE\"],\"mode\":\"DINING\",\"time\":\"08:30\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakfast.time").value("08:30"))
                .andExpect(jsonPath("$.breakfast.items.length()").value(2));
        putJson("/api/guest/breakfast", guest, "{\"items\":[],\"mode\":\"DINING\",\"time\":\"08:30\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportReachesStaffAndIsPickedUpAutomatically() throws Exception {
        String guest = guestToken("HX-4821", "Lind");
        postJson("/api/guest/reports", guest, "{\"category\":\"ROOM\",\"text\":\"Elementet är kallt\",\"hasPhoto\":false}")
                .andExpect(jsonPath("$.report.id").value(214))
                .andExpect(jsonPath("$.report.status").value("RECEIVED"));

        String staff = staffToken();
        mvc.perform(get("/api/staff/dashboard").header("Authorization", "Bearer " + staff))
                .andExpect(jsonPath("$.reports[0].id").value(214))
                .andExpect(jsonPath("$.log[*].en", hasItem("Received case #214 from room 12, notified staff")));

        String status = "RECEIVED";
        for (int i = 0; i < 40 && status.equals("RECEIVED"); i++) {
            Thread.sleep(100);
            status = JsonPath.read(mvc.perform(get("/api/guest/me").header("Authorization", "Bearer " + guest))
                    .andReturn().getResponse().getContentAsString(), "$.report.status");
        }
        org.junit.jupiter.api.Assertions.assertEquals("IN_PROGRESS", status);

        postJson("/api/staff/reports/214/advance", staff, "{}")
                .andExpect(jsonPath("$.status").value("FIXED"));
    }

    @Test
    void checkoutNeedsCheckInAndRecordsRating() throws Exception {
        String guest = guestToken("HX-6102", "Smith");
        postJson("/api/guest/check-out", guest, "{\"rating\":5}").andExpect(status().isConflict());
        postJson("/api/guest/check-in", guest, "{}");
        postJson("/api/guest/check-out", guest, "{\"rating\":5}")
                .andExpect(jsonPath("$.checkedOut").value(true))
                .andExpect(jsonPath("$.rating").value(5));
    }

    // ---- helpers -----------------------------------------------------------

    private String guestToken(String booking, String lastName) throws Exception {
        String body = postJson("/api/auth/guest", null,
                "{\"bookingNumber\":\"%s\",\"lastName\":\"%s\"}".formatted(booking, lastName))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String staffToken() throws Exception {
        String body = postJson("/api/auth/staff", null, "{\"username\":\"lisa.b\",\"pin\":\"2026\"}")
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private long slotId(String token, String time) throws Exception {
        String body = mvc.perform(get("/api/guest/sauna/slots").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.<java.util.List<Number>>read(body, "$[?(@.time == '" + time + "')].id").getFirst();
        return id.longValue();
    }

    private void assertBooked(String token, String time, int expected) throws Exception {
        mvc.perform(get("/api/guest/sauna/slots").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[?(@.time == '" + time + "')].booked", hasItem(expected)));
    }

    private ResultActions postJson(String url, String token, String json) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request);
    }

    private ResultActions putJson(String url, String token, String json) throws Exception {
        return mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json)
                .header("Authorization", "Bearer " + token));
    }
}
