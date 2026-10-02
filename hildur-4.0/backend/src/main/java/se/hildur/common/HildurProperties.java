package se.hildur.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Settings from application.properties, prefix "hildur".
 *
 * @param timeZone          the hotel's time zone, e.g. Europe/Stockholm
 * @param wifiNetwork       guest Wi-Fi network name
 * @param wifiPassword      guest Wi-Fi password (only sent to checked-in guests)
 * @param onDutyStaff       username of the staff member who picks up new reports
 * @param autoAssignDelay   how long before a new report is automatically marked "in progress"
 * @param sessionTtl        how long a login lasts
 * @param allowedOrigins    browser origins allowed to call the API (the React dev server)
 */
@ConfigurationProperties(prefix = "hildur")
public record HildurProperties(
        String timeZone,
        String wifiNetwork,
        String wifiPassword,
        String onDutyStaff,
        Duration autoAssignDelay,
        Duration sessionTtl,
        List<String> allowedOrigins) {
}
