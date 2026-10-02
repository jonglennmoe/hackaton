package se.hildur.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * One time slot in the sauna today, e.g. 06:00 with 8 seats.
 * {@code booked} counts every seat taken, including walk-ins booked at the desk.
 */
@Entity
public class SaunaSlot {

    public static final int DEFAULT_CAPACITY = 8;

    @Id
    @GeneratedValue
    private Long id;
    private String time;
    private int capacity;
    private int booked;

    /** Optimistic locking: two guests grabbing the last seat at once can't both win. */
    @Version
    private long version;

    protected SaunaSlot() {
    }

    public SaunaSlot(String time, int capacity, int booked) {
        this.time = time;
        this.capacity = capacity;
        this.booked = booked;
    }

    public boolean isFull() {
        return booked >= capacity;
    }

    public int seatsLeft() {
        return Math.max(0, capacity - booked);
    }

    public void takeSeat() {
        if (isFull()) {
            throw new IllegalStateException("Slot " + time + " is full");
        }
        booked++;
    }

    public void releaseSeat() {
        if (booked > 0) {
            booked--;
        }
    }

    public Long getId() { return id; }
    public String getTime() { return time; }
    public int getCapacity() { return capacity; }
    public int getBooked() { return booked; }
    public void setBooked(int booked) { this.booked = booked; }
}
