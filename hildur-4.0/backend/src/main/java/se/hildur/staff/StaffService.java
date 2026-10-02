package se.hildur.staff;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hildur.activity.ActivityLogService;
import se.hildur.common.ApiException;
import se.hildur.domain.Booking;
import se.hildur.domain.BookingRepository;
import se.hildur.domain.SaunaSlotRepository;
import se.hildur.domain.StaffUser;
import se.hildur.domain.StaffUserRepository;
import se.hildur.report.ReportService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Builds the one-screen staff overview. */
@Service
public class StaffService {

    public record Me(String name, String roleSv, String roleEn) {
    }

    /** One sauna slot with the app guests who booked it, e.g. ["Anna L. (12)"]. */
    public record SlotOverview(Long id, String time, int capacity, int booked, List<String> appGuests) {
    }

    public record Dashboard(Me me, List<SlotOverview> slots, List<ReportService.ReportDto> reports,
                            List<ActivityLogService.LogEntryDto> log) {
    }

    private final StaffUserRepository staff;
    private final SaunaSlotRepository slots;
    private final BookingRepository bookings;
    private final ReportService reports;
    private final ActivityLogService activityLog;

    public StaffService(StaffUserRepository staff, SaunaSlotRepository slots, BookingRepository bookings,
                        ReportService reports, ActivityLogService activityLog) {
        this.staff = staff;
        this.slots = slots;
        this.bookings = bookings;
        this.reports = reports;
        this.activityLog = activityLog;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(String username) {
        StaffUser user = staff.findById(username).orElseThrow(() -> ApiException.unauthorized("notLoggedIn"));
        Map<Long, List<String>> guestsBySlot = bookings.findAll().stream()
                .filter(b -> b.getSaunaSlot() != null)
                .collect(Collectors.groupingBy(b -> b.getSaunaSlot().getId(),
                        Collectors.mapping(Booking::tag, Collectors.toList())));
        List<SlotOverview> slotOverview = slots.findAllByOrderByTimeAsc().stream()
                .map(s -> new SlotOverview(s.getId(), s.getTime(), s.getCapacity(), s.getBooked(),
                        guestsBySlot.getOrDefault(s.getId(), List.of())))
                .toList();
        return new Dashboard(new Me(user.getDisplayName(), user.getRoleSv(), user.getRoleEn()),
                slotOverview, reports.all(), activityLog.latest());
    }
}
