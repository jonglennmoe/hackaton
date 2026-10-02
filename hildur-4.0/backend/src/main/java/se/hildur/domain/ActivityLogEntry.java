package se.hildur.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

/** One line in the staff activity log: who did what, and when. Never edited, only added. */
@Entity
public class ActivityLogEntry {

    @Id
    @GeneratedValue
    private Long id;
    private LocalDateTime at;
    private String who;
    private String textSv;
    private String textEn;

    protected ActivityLogEntry() {
    }

    public ActivityLogEntry(LocalDateTime at, String who, String textSv, String textEn) {
        this.at = at;
        this.who = who;
        this.textSv = textSv;
        this.textEn = textEn;
    }

    public Long getId() { return id; }
    public LocalDateTime getAt() { return at; }
    public String getWho() { return who; }
    public String getTextSv() { return textSv; }
    public String getTextEn() { return textEn; }
}
