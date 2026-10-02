package se.hildur.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SaunaSlotRepository extends JpaRepository<SaunaSlot, Long> {

    List<SaunaSlot> findAllByOrderByTimeAsc();
}
