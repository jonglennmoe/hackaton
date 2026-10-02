package se.hildur.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.time.LocalDateTime;

/** A problem a guest reported, e.g. "the radiator is cold". Staff move it through its statuses. */
@Entity
public class ProblemReport {

    public enum Category { ROOM, WIFI, SAUNA, OTHER }

    /** RECEIVED → IN_PROGRESS → FIXED, then (staff tapping again) back to RECEIVED. */
    public enum Status {
        RECEIVED("Mottaget", "Received"),
        IN_PROGRESS("Pågår", "In progress"),
        FIXED("Åtgärdat", "Fixed");

        public final String sv;
        public final String en;

        Status(String sv, String en) {
            this.sv = sv;
            this.en = en;
        }

        public Status next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Case number shown to guests ("#214"). The sequence starts at 211, the first seeded report. */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "report_seq")
    @SequenceGenerator(name = "report_seq", initialValue = 211, allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String room;

    /** Null for reports that didn't come through the app. */
    private String bookingNumber;

    @Column(length = 280)
    private String textSv;

    @Column(length = 280)
    private String textEn;

    private boolean hasPhoto;

    @Enumerated(EnumType.STRING)
    private Status status = Status.RECEIVED;

    private LocalDateTime receivedAt;
    private LocalDateTime inProgressAt;
    private LocalDateTime fixedAt;

    protected ProblemReport() {
    }

    /** A report written by a guest: the text is shown as-is in both languages. */
    public ProblemReport(Category category, String room, String bookingNumber,
                         String text, boolean hasPhoto, LocalDateTime receivedAt) {
        this(category, room, text, text, receivedAt);
        this.bookingNumber = bookingNumber;
        this.hasPhoto = hasPhoto;
    }

    /** A report with a translated text (used for seed data). */
    public ProblemReport(Category category, String room, String textSv, String textEn, LocalDateTime receivedAt) {
        this.category = category;
        this.room = room;
        this.textSv = textSv;
        this.textEn = textEn;
        this.receivedAt = receivedAt;
    }

    /** Moves to the given status and stamps the time it happened. */
    public void changeStatus(Status newStatus, LocalDateTime at) {
        this.status = newStatus;
        switch (newStatus) {
            case RECEIVED -> {
                inProgressAt = null;
                fixedAt = null;
            }
            case IN_PROGRESS -> {
                inProgressAt = at;
                fixedAt = null;
            }
            case FIXED -> {
                if (inProgressAt == null) {
                    inProgressAt = at;
                }
                fixedAt = at;
            }
        }
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getRoom() { return room; }
    public String getBookingNumber() { return bookingNumber; }
    public String getTextSv() { return textSv; }
    public String getTextEn() { return textEn; }
    public boolean isHasPhoto() { return hasPhoto; }
    public Status getStatus() { return status; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public LocalDateTime getInProgressAt() { return inProgressAt; }
    public LocalDateTime getFixedAt() { return fixedAt; }
}
