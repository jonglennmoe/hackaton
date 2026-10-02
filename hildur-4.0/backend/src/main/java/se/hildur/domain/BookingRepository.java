package se.hildur.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data writes the implementation at startup by reading the method names. */
public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findBySaunaSlot(SaunaSlot slot);
}
