package se.hildur.guest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hildur.activity.ActivityLogService;
import se.hildur.common.ApiException;
import se.hildur.common.HildurProperties;
import se.hildur.domain.Booking;
import se.hildur.domain.BookingRepository;
import se.hildur.domain.BreakfastItem;
import se.hildur.domain.BreakfastOrder;
import se.hildur.domain.BreakfastOrderRepository;
import se.hildur.domain.SaunaSlot;
import se.hildur.domain.SaunaSlotRepository;
import se.hildur.report.ReportService;

import java.util.List;

/**
 * Everything a logged-in guest can do. Every method takes the guest's own
 * booking number from their session, so a guest can only ever touch their own stay.
 */
@Service
public class GuestService {

    private final BookingRepository bookings;
    private final SaunaSlotRepository slots;
    private final BreakfastOrderRepository breakfasts;
    private final ReportService reports;
    private final ActivityLogService activityLog;
    private final HildurProperties properties;

    public GuestService(BookingRepository bookings, SaunaSlotRepository slots, BreakfastOrderRepository breakfasts,
                        ReportService reports, ActivityLogService activityLog, HildurProperties properties) {
        this.bookings = bookings;
        this.slots = slots;
        this.breakfasts = breakfasts;
        this.reports = reports;
        this.activityLog = activityLog;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public GuestDtos.GuestState state(String bookingNumber) {
        Booking b = booking(bookingNumber);
        SaunaSlot slot = b.getSaunaSlot();
        GuestDtos.Breakfast breakfast = breakfasts.findById(bookingNumber)
                .map(o -> new GuestDtos.Breakfast(o.getItems(), o.getMode(), o.getTime()))
                .orElse(null);
        return new GuestDtos.GuestState(b.getBookingNumber(), b.getFirstName(), b.getLastName(), b.getRoom(),
                b.getEmail(), b.isCheckedIn(), b.isCheckedOut(), b.getRating(), b.isNotifyAurora(),
                slot == null ? null : slot.getId(), slot == null ? null : slot.getTime(),
                breakfast, reports.latestFor(bookingNumber).orElse(null), reports.onDutyFirstName());
    }

    @Transactional
    public GuestDtos.GuestState checkIn(String bookingNumber) {
        Booking b = booking(bookingNumber);
        if (!b.isCheckedIn()) {
            b.checkIn();
            activityLog.log(b.tag(), "Checkade in", "Checked in");
        }
        return state(bookingNumber);
    }

    // ---- Sauna -------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<GuestDtos.Slot> slots() {
        return slots.findAllByOrderByTimeAsc().stream()
                .map(s -> new GuestDtos.Slot(s.getId(), s.getTime(), s.getCapacity(), s.getBooked()))
                .toList();
    }

    /** Books a slot. If the guest already had another slot, that seat is given back first. */
    @Transactional
    public GuestDtos.GuestState bookSauna(String bookingNumber, long slotId) {
        Booking b = booking(bookingNumber);
        SaunaSlot wanted = slots.findById(slotId).orElseThrow(() -> ApiException.notFound("slotNotFound"));
        SaunaSlot current = b.getSaunaSlot();
        if (current != null && current.getId().equals(wanted.getId())) {
            return state(bookingNumber);
        }
        if (wanted.isFull()) {
            throw ApiException.conflict("slotFull");
        }
        if (current != null) {
            current.releaseSeat();
        }
        wanted.takeSeat();
        b.setSaunaSlot(wanted);
        activityLog.log(b.tag(), "Bokade bastu " + wanted.getTime(), "Booked sauna " + wanted.getTime());
        return state(bookingNumber);
    }

    @Transactional
    public GuestDtos.GuestState cancelSauna(String bookingNumber) {
        Booking b = booking(bookingNumber);
        SaunaSlot current = b.getSaunaSlot();
        if (current != null) {
            current.releaseSeat();
            b.setSaunaSlot(null);
            activityLog.log(b.tag(), "Avbokade bastu " + current.getTime(), "Cancelled sauna " + current.getTime());
        }
        return state(bookingNumber);
    }

    // ---- Breakfast ---------------------------------------------------------

    @Transactional
    public GuestDtos.GuestState orderBreakfast(String bookingNumber, GuestDtos.BreakfastRequest request) {
        if (!BreakfastItem.TIMES.contains(request.time())) {
            throw ApiException.badRequest("invalidTime");
        }
        List<BreakfastItem> items = request.items().stream().distinct().toList();
        Booking b = booking(bookingNumber);
        BreakfastOrder order = breakfasts.findById(bookingNumber).orElseGet(() -> new BreakfastOrder(b));
        order.update(items, request.mode(), request.time());
        breakfasts.save(order);
        activityLog.log(b.tag(),
                "Beställde frukost till %s (%d st)".formatted(request.time(), items.size()),
                "Ordered breakfast for %s (%d items)".formatted(request.time(), items.size()));
        return state(bookingNumber);
    }

    // ---- Problem reports ---------------------------------------------------

    @Transactional
    public GuestDtos.GuestState report(String bookingNumber, GuestDtos.ReportRequest request) {
        reports.create(booking(bookingNumber), request.category(), request.text(), request.hasPhoto());
        return state(bookingNumber);
    }

    // ---- Northern lights ---------------------------------------------------

    @Transactional
    public GuestDtos.GuestState setAuroraAlert(String bookingNumber, boolean notify) {
        booking(bookingNumber).setNotifyAurora(notify);
        return state(bookingNumber);
    }

    /** Tonight's forecast. A real hotel would fetch this from a space-weather and weather service. */
    public GuestDtos.Forecast forecast() {
        return new GuestDtos.Forecast("LOW", "CLOUDY", 2, 85, "19:40", "06:10");
    }

    // ---- Wi-Fi -------------------------------------------------------------

    /** The password only leaves the server for a guest who has checked in. */
    @Transactional(readOnly = true)
    public GuestDtos.Wifi wifi(String bookingNumber) {
        if (!booking(bookingNumber).isCheckedIn()) {
            throw ApiException.forbidden("checkInFirst");
        }
        return new GuestDtos.Wifi(properties.wifiNetwork(), properties.wifiPassword());
    }

    // ---- My data -----------------------------------------------------------

    @Transactional
    public void requestExport(String bookingNumber) {
        activityLog.log(booking(bookingNumber).tag(), "Begärde export av sina data", "Requested data export");
    }

    @Transactional
    public void requestDeletion(String bookingNumber) {
        activityLog.log(booking(bookingNumber).tag(), "Begärde radering av sina data", "Requested data deletion");
    }

    // ---- Check-out ---------------------------------------------------------

    @Transactional
    public GuestDtos.GuestState checkOut(String bookingNumber, Integer rating) {
        Booking b = booking(bookingNumber);
        if (!b.isCheckedIn()) {
            throw ApiException.conflict("checkInFirst");
        }
        if (!b.isCheckedOut()) {
            b.checkOut(rating);
            activityLog.log(b.tag(),
                    rating == null ? "Checkade ut" : "Checkade ut, betyg %d/5".formatted(rating),
                    rating == null ? "Checked out" : "Checked out, rated %d/5".formatted(rating));
        }
        return state(bookingNumber);
    }

    /** Demo button "Start over": undo the whole stay so the flow can be tried again. */
    @Transactional
    public GuestDtos.GuestState restart(String bookingNumber) {
        Booking b = booking(bookingNumber);
        if (b.getSaunaSlot() != null) {
            b.getSaunaSlot().releaseSeat();
        }
        b.resetStay();
        breakfasts.findById(bookingNumber).ifPresent(breakfasts::delete);
        reports.deleteAllFor(bookingNumber);
        return state(bookingNumber);
    }

    private Booking booking(String bookingNumber) {
        return bookings.findById(bookingNumber).orElseThrow(() -> ApiException.notFound("bookingNotFound"));
    }
}
