package se.hildur.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntry, Long> {

    List<ActivityLogEntry> findTop50ByOrderByAtDescIdDesc();
}
