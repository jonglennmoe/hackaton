package se.hildur.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProblemReportRepository extends JpaRepository<ProblemReport, Long> {

    List<ProblemReport> findAllByOrderByIdDesc();

    Optional<ProblemReport> findTopByOrderByIdDesc();

    Optional<ProblemReport> findTopByBookingNumberOrderByIdDesc(String bookingNumber);

    List<ProblemReport> findByBookingNumber(String bookingNumber);
}
