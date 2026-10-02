package se.hildur.activity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hildur.common.HotelClock;
import se.hildur.domain.ActivityLogEntry;
import se.hildur.domain.ActivityLogRepository;

import java.util.List;

/**
 * Hildur's promise "every change to prices and bookings is logged" lives here.
 * Each entry is written in Swedish and English so staff can switch language.
 */
@Service
public class ActivityLogService {

    public record LogEntryDto(String time, String who, String sv, String en) {
    }

    private final ActivityLogRepository repository;
    private final HotelClock clock;

    public ActivityLogService(ActivityLogRepository repository, HotelClock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public void log(String who, String sv, String en) {
        repository.save(new ActivityLogEntry(clock.now(), who, sv, en));
    }

    @Transactional(readOnly = true)
    public List<LogEntryDto> latest() {
        return repository.findTop50ByOrderByAtDescIdDesc().stream()
                .map(e -> new LogEntryDto(HotelClock.hhmm(e.getAt()), e.getWho(), e.getTextSv(), e.getTextEn()))
                .toList();
    }
}
