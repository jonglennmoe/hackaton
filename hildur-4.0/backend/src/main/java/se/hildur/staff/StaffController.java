package se.hildur.staff;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.hildur.auth.AuthInterceptor;
import se.hildur.auth.Session;
import se.hildur.report.ReportService;

/** The staff API. Only STAFF tokens get past {@link AuthInterceptor} to reach these methods. */
@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;
    private final ReportService reports;

    public StaffController(StaffService staffService, ReportService reports) {
        this.staffService = staffService;
        this.reports = reports;
    }

    @GetMapping("/dashboard")
    public StaffService.Dashboard dashboard(@RequestAttribute(AuthInterceptor.SESSION) Session session) {
        return staffService.dashboard(session.subject());
    }

    @PostMapping("/reports/{id}/advance")
    public ReportService.ReportDto advance(@RequestAttribute(AuthInterceptor.SESSION) Session session,
                                           @PathVariable long id) {
        return reports.advance(id, session.name());
    }
}
