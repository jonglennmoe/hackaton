package se.hildur.common;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * The hotel's local time. Everything is shown in the hotel's time zone,
 * no matter where the server runs (e.g. a cloud region in UTC).
 */
@Component
public class HotelClock {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock clock;

    public HotelClock(HildurProperties properties) {
        this.clock = Clock.system(ZoneId.of(properties.timeZone()));
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** "09:30", or null when there is no time. */
    public static String hhmm(LocalDateTime time) {
        return time == null ? null : time.format(HH_MM);
    }
}
