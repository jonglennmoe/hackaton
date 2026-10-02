package se.hildur.report;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import se.hildur.activity.ActivityLogService;
import se.hildur.common.ApiException;
import se.hildur.common.HildurProperties;
import se.hildur.common.HotelClock;
import se.hildur.domain.Booking;
import se.hildur.domain.ProblemReport;
import se.hildur.domain.ProblemReportRepository;
import se.hildur.domain.StaffUser;
import se.hildur.domain.StaffUserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Problem reports: guests create them, staff move them along. */
@Service
public class ReportService {

    /** What both guests and staff see about a report. Times are "HH:mm". */
    public record ReportDto(Long id, ProblemReport.Category category, String room,
                            String textSv, String textEn, boolean hasPhoto, ProblemReport.Status status,
                            String receivedAt, String inProgressAt, String fixedAt) {

        static ReportDto from(ProblemReport r) {
            return new ReportDto(r.getId(), r.getCategory(), r.getRoom(), r.getTextSv(), r.getTextEn(),
                    r.isHasPhoto(), r.getStatus(), HotelClock.hhmm(r.getReceivedAt()),
                    HotelClock.hhmm(r.getInProgressAt()), HotelClock.hhmm(r.getFixedAt()));
        }
    }

    private final ProblemReportRepository reports;
    private final StaffUserRepository staff;
    private final ActivityLogService activityLog;
    private final HotelClock clock;
    private final TaskScheduler scheduler;
    private final TransactionTemplate transaction;
    private final HildurProperties properties;

    public ReportService(ProblemReportRepository reports, StaffUserRepository staff, ActivityLogService activityLog,
                         HotelClock clock, TaskScheduler scheduler, TransactionTemplate transaction,
                         HildurProperties properties) {
        this.reports = reports;
        this.staff = staff;
        this.activityLog = activityLog;
        this.clock = clock;
        this.scheduler = scheduler;
        this.transaction = transaction;
        this.properties = properties;
    }

    @Transactional
    public ReportDto create(Booking booking, ProblemReport.Category category, String text, boolean hasPhoto) {
        ProblemReport report = reports.save(new ProblemReport(category, booking.getRoom(), booking.getBookingNumber(),
                text.trim(), hasPhoto, clock.now()));
        activityLog.log("Hildur",
                "Tog emot ärende #%d från rum %s, meddelade personalen".formatted(report.getId(), booking.getRoom()),
                "Received case #%d from room %s, notified staff".formatted(report.getId(), booking.getRoom()));
        // In a few seconds the on-duty staff member "picks it up" – the guest sees "Lisa is on her way".
        Long id = report.getId();
        scheduler.schedule(() -> transaction.executeWithoutResult(tx -> autoAssign(id)),
                Instant.now().plus(properties.autoAssignDelay()));
        return ReportDto.from(report);
    }

    /** Staff tapped the status chip: RECEIVED → IN_PROGRESS → FIXED → RECEIVED. */
    @Transactional
    public ReportDto advance(long id, String staffName) {
        ProblemReport report = reports.findById(id).orElseThrow(() -> ApiException.notFound("reportNotFound"));
        changeStatus(report, report.getStatus().next(), staffName);
        return ReportDto.from(report);
    }

    @Transactional(readOnly = true)
    public List<ReportDto> all() {
        return reports.findAllByOrderByIdDesc().stream().map(ReportDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ReportDto> latestFor(String bookingNumber) {
        return reports.findTopByBookingNumberOrderByIdDesc(bookingNumber).map(ReportDto::from);
    }

    @Transactional
    public void deleteAllFor(String bookingNumber) {
        reports.deleteAll(reports.findByBookingNumber(bookingNumber));
    }

    private void autoAssign(Long id) {
        reports.findById(id)
                .filter(r -> r.getStatus() == ProblemReport.Status.RECEIVED)
                .ifPresent(r -> changeStatus(r, ProblemReport.Status.IN_PROGRESS, onDutyName()));
    }

    private void changeStatus(ProblemReport report, ProblemReport.Status status, String who) {
        report.changeStatus(status, clock.now());
        activityLog.log(who,
                "Ändrade #%d till %s".formatted(report.getId(), status.sv),
                "Changed #%d to %s".formatted(report.getId(), status.en));
    }

    /** First name of whoever picks up new reports, for "Lisa is on the way". */
    @Transactional(readOnly = true)
    public String onDutyFirstName() {
        return onDutyName().split(" ")[0];
    }

    private String onDutyName() {
        return staff.findById(properties.onDutyStaff()).map(StaffUser::getDisplayName).orElse("Staff");
    }
}
