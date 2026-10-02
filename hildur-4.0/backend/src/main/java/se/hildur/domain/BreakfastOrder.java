package se.hildur.domain;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.MapsId;

import java.util.ArrayList;
import java.util.List;

/** Tomorrow's breakfast for one booking. One order per booking; ordering again replaces it. */
@Entity
public class BreakfastOrder {

    public enum Mode { ROOM, DINING }

    @Id
    private String bookingNumber;

    @MapsId
    @OneToOne
    private Booking booking;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<BreakfastItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Mode mode;

    private String time;

    protected BreakfastOrder() {
    }

    public BreakfastOrder(Booking booking) {
        this.booking = booking;
    }

    public void update(List<BreakfastItem> items, Mode mode, String time) {
        this.items = new ArrayList<>(items);
        this.mode = mode;
        this.time = time;
    }

    public String getBookingNumber() { return bookingNumber; }
    public Booking getBooking() { return booking; }
    public List<BreakfastItem> getItems() { return items; }
    public Mode getMode() { return mode; }
    public String getTime() { return time; }
}
