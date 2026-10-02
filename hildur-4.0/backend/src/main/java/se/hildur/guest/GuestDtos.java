package se.hildur.guest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import se.hildur.domain.BreakfastItem;
import se.hildur.domain.BreakfastOrder;
import se.hildur.domain.ProblemReport;
import se.hildur.report.ReportService;

import java.util.List;

/**
 * DTOs ("data transfer objects"): the exact JSON shapes the guest API sends
 * and receives. Java records are perfect for this – immutable, with
 * constructor, getters, equals and toString written for us.
 */
public final class GuestDtos {

    private GuestDtos() {
    }

    /** Everything the guest app needs to draw its screens. */
    public record GuestState(String bookingNumber, String firstName, String lastName, String room, String email,
                             boolean checkedIn, boolean checkedOut, Integer rating, boolean notifyAurora,
                             Long saunaSlotId, String saunaTime, Breakfast breakfast,
                             ReportService.ReportDto report, String onDutyStaff) {
    }

    public record Breakfast(List<BreakfastItem> items, BreakfastOrder.Mode mode, String time) {
    }

    public record Slot(Long id, String time, int capacity, int booked) {
    }

    public record Wifi(String network, String password) {
    }

    /** level: LOW/MEDIUM/HIGH, sky: CLEAR/CLOUDY. kp is the geomagnetic index (0–9). */
    public record Forecast(String level, String sky, int kp, int cloudCover, String darkFrom, String darkTo) {
    }

    // ---- request bodies ----------------------------------------------------

    public record SaunaRequest(@NotNull Long slotId) {
    }

    public record BreakfastRequest(@NotEmpty List<BreakfastItem> items, @NotNull BreakfastOrder.Mode mode,
                                   @NotBlank String time) {
    }

    public record ReportRequest(@NotNull ProblemReport.Category category, @NotBlank @Size(max = 280) String text,
                                boolean hasPhoto) {
    }

    /** "enabled", not "notify": every Java object already has a notify() method. */
    public record AuroraRequest(boolean enabled) {
    }

    public record CheckoutRequest(@Min(1) @Max(5) Integer rating) {
    }
}
