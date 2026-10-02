package se.hildur.guest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import se.hildur.auth.AuthInterceptor;
import se.hildur.auth.Session;

import java.util.List;

/**
 * The guest API. The controller's only job is HTTP: read the request, call the
 * service, return JSON. The logged-in {@link Session} is put on the request by
 * {@link AuthInterceptor}, so we always know whose booking this is.
 */
@RestController
@RequestMapping("/api/guest")
public class GuestController {

    private final GuestService guests;

    public GuestController(GuestService guests) {
        this.guests = guests;
    }

    @GetMapping("/me")
    public GuestDtos.GuestState me(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        return guests.state(session.subject());
    }

    @PostMapping("/check-in")
    public GuestDtos.GuestState checkIn(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        return guests.checkIn(session.subject());
    }

    @GetMapping("/sauna/slots")
    public List<GuestDtos.Slot> slots() {
        return guests.slots();
    }

    @PostMapping("/sauna/booking")
    public GuestDtos.GuestState bookSauna(@RequestAttribute(AuthInterceptor.SESSION) Session session,
                                          @Valid @RequestBody GuestDtos.SaunaRequest body) {
        return guests.bookSauna(session.subject(), body.slotId());
    }

    @DeleteMapping("/sauna/booking")
    public GuestDtos.GuestState cancelSauna(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        return guests.cancelSauna(session.subject());
    }

    @PutMapping("/breakfast")
    public GuestDtos.GuestState breakfast(@RequestAttribute(AuthInterceptor.SESSION) Session session,
                                          @Valid @RequestBody GuestDtos.BreakfastRequest body) {
        return guests.orderBreakfast(session.subject(), body);
    }

    @PostMapping("/reports")
    public GuestDtos.GuestState report(@RequestAttribute(AuthInterceptor.SESSION) Session session,
                                       @Valid @RequestBody GuestDtos.ReportRequest body) {
        return guests.report(session.subject(), body);
    }

    @GetMapping("/aurora")
    public GuestDtos.Forecast forecast() {
        return guests.forecast();
    }

    @PutMapping("/aurora")
    public GuestDtos.GuestState aurora(@RequestAttribute(AuthInterceptor.SESSION) Session session,
                                       @RequestBody GuestDtos.AuroraRequest body) {
        return guests.setAuroraAlert(session.subject(), body.enabled());
    }

    @GetMapping("/wifi")
    public GuestDtos.Wifi wifi(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        return guests.wifi(session.subject());
    }

    @PostMapping("/data/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void export(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        guests.requestExport(session.subject());
    }

    @PostMapping("/data/deletion")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deletion(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        guests.requestDeletion(session.subject());
    }

    @PostMapping("/check-out")
    public GuestDtos.GuestState checkOut(@RequestAttribute(AuthInterceptor.SESSION) Session session,
                                         @Valid @RequestBody GuestDtos.CheckoutRequest body) {
        return guests.checkOut(session.subject(), body.rating());
    }

    @PostMapping("/restart")
    public GuestDtos.GuestState restart(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        return guests.restart(session.subject());
    }
}
