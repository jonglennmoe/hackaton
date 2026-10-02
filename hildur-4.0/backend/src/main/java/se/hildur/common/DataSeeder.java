package se.hildur.common;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.hildur.auth.PinHasher;
import se.hildur.domain.ActivityLogEntry;
import se.hildur.domain.ActivityLogRepository;
import se.hildur.domain.Booking;
import se.hildur.domain.BookingRepository;
import se.hildur.domain.ProblemReport;
import se.hildur.domain.ProblemReport.Category;
import se.hildur.domain.ProblemReportRepository;
import se.hildur.domain.SaunaSlot;
import se.hildur.domain.SaunaSlotRepository;
import se.hildur.domain.StaffUser;
import se.hildur.domain.StaffUserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Fills the in-memory database with demo data every time the app starts.
 *
 * Demo logins:
 *   guests  HX-4821 / Lind, HX-5530 / Berg, HX-6102 / Smith
 *   staff   lisa.b / 2026
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final BookingRepository bookings;
    private final SaunaSlotRepository slots;
    private final ProblemReportRepository reports;
    private final StaffUserRepository staff;
    private final ActivityLogRepository log;
    private final HotelClock clock;

    public DataSeeder(BookingRepository bookings, SaunaSlotRepository slots, ProblemReportRepository reports,
                      StaffUserRepository staff, ActivityLogRepository log, HotelClock clock) {
        this.bookings = bookings;
        this.slots = slots;
        this.reports = reports;
        this.staff = staff;
        this.log = log;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        bookings.saveAll(List.of(
                new Booking("HX-4821", "Anna", "Lind", "12", "anna.lind@mail.se"),
                new Booking("HX-5530", "Erik", "Berg", "7", "erik.berg@mail.se"),
                new Booking("HX-6102", "Sam", "Smith", "4", "sam.smith@mail.com")));

        // Seats already taken by guests who booked at the front desk.
        slots.saveAll(List.of(
                slot("06:00", 5), slot("07:00", 8), slot("08:00", 2), slot("12:00", 4), slot("17:00", 7),
                slot("18:00", 8), slot("19:00", 4), slot("20:00", 0), slot("21:00", 6)));

        String salt = PinHasher.newSalt();
        staff.save(new StaffUser("lisa.b", "Lisa B.", "reception", "front desk", salt, PinHasher.hash(salt, "2026")));

        // Saved in order, so they get case numbers #211, #212, #213.
        ProblemReport wifi = new ProblemReport(Category.WIFI, "4",
                "Långsamt nät på rummet.", "Slow internet in the room.", today(7, 50));
        wifi.changeStatus(ProblemReport.Status.FIXED, today(8, 30));
        reports.save(wifi);
        ProblemReport sauna = new ProblemReport(Category.SAUNA, "–",
                "Termometern visar 40 grader, känns som 90.", "Thermometer says 40 degrees, feels like 90.", today(8, 55));
        sauna.changeStatus(ProblemReport.Status.IN_PROGRESS, today(9, 10));
        reports.save(sauna);
        reports.save(new ProblemReport(Category.ROOM, "7", "Kranen droppar.", "The tap is dripping.", today(9, 28)));

        log.saveAll(List.of(
                new ActivityLogEntry(today(8, 12), "Lisa B.", "Ändrade frukostpris: 165 → 175 kr", "Changed breakfast price: 165 → 175 SEK"),
                new ActivityLogEntry(today(8, 40), "System", "Bastu 07:00 fullbokad", "Sauna 07:00 fully booked"),
                new ActivityLogEntry(today(9, 5), "Erik N.", "Avbokade bastu 06:00 för rum 3", "Cancelled sauna 06:00 for room 3"),
                new ActivityLogEntry(today(9, 30), "Hildur", "Skickade ärende #213 till Lisa B.", "Sent case #213 to Lisa B.")));
    }

    private static SaunaSlot slot(String time, int booked) {
        return new SaunaSlot(time, SaunaSlot.DEFAULT_CAPACITY, booked);
    }

    private LocalDateTime today(int hour, int minute) {
        LocalDate date = clock.now().toLocalDate();
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }
}
