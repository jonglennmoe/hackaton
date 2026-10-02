package se.hildur.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * A guest's stay. The booking number (e.g. "HX-4821") is the primary key and,
 * together with the last name, is what a guest logs in with.
 */
@Entity
public class Booking {

    @Id
    private String bookingNumber;
    private String firstName;
    private String lastName;
    private String room;
    private String email;
    private boolean checkedIn;
    private boolean checkedOut;
    private Integer rating;
    private boolean notifyAurora;

    /** The guest's sauna slot today, or null. Many bookings can share one slot. */
    @ManyToOne(fetch = FetchType.EAGER)
    private SaunaSlot saunaSlot;

    protected Booking() {
        // required by JPA
    }

    public Booking(String bookingNumber, String firstName, String lastName, String room, String email) {
        this.bookingNumber = bookingNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.room = room;
        this.email = email;
    }

    /** Short label used in the staff activity log, e.g. "Anna L. (12)". */
    public String tag() {
        return "%s %s. (%s)".formatted(firstName, lastName.charAt(0), room);
    }

    public void checkIn() {
        this.checkedIn = true;
    }

    public void checkOut(Integer rating) {
        this.checkedOut = true;
        this.rating = rating;
    }

    /** Demo "start over": back to the state before check-in. */
    public void resetStay() {
        checkedIn = false;
        checkedOut = false;
        rating = null;
        notifyAurora = false;
        saunaSlot = null;
    }

    public String getBookingNumber() { return bookingNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getRoom() { return room; }
    public String getEmail() { return email; }
    public boolean isCheckedIn() { return checkedIn; }
    public boolean isCheckedOut() { return checkedOut; }
    public Integer getRating() { return rating; }
    public boolean isNotifyAurora() { return notifyAurora; }
    public void setNotifyAurora(boolean notifyAurora) { this.notifyAurora = notifyAurora; }
    public SaunaSlot getSaunaSlot() { return saunaSlot; }
    public void setSaunaSlot(SaunaSlot saunaSlot) { this.saunaSlot = saunaSlot; }
}
