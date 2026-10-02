package se.hildur.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hildur.activity.ActivityLogService;
import se.hildur.common.ApiException;
import se.hildur.common.HildurProperties;
import se.hildur.domain.Booking;
import se.hildur.domain.BookingRepository;
import se.hildur.domain.StaffUser;
import se.hildur.domain.StaffUserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two separate doors: guests log in with booking number + last name,
 * staff log in with username + PIN. Both get a random token back that the
 * browser sends as "Authorization: Bearer &lt;token&gt;" on every request.
 */
@Service
public class AuthService {

    public record LoginResult(String token, Session.Role role, String name) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    /** token → session. Kept in memory, like the database. */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final BookingRepository bookings;
    private final StaffUserRepository staff;
    private final ActivityLogService activityLog;
    private final HildurProperties properties;

    public AuthService(BookingRepository bookings, StaffUserRepository staff,
                       ActivityLogService activityLog, HildurProperties properties) {
        this.bookings = bookings;
        this.staff = staff;
        this.activityLog = activityLog;
        this.properties = properties;
    }

    @Transactional
    public LoginResult loginGuest(String bookingNumber, String lastName) {
        Booking booking = bookings.findById(bookingNumber.trim().toUpperCase(Locale.ROOT))
                .filter(b -> b.getLastName().equalsIgnoreCase(lastName.trim()))
                .orElseThrow(() -> ApiException.unauthorized("errGuest"));
        String name = booking.tag();
        activityLog.log(name, "Loggade in", "Logged in");
        return start(Session.Role.GUEST, booking.getBookingNumber(), name);
    }

    @Transactional
    public LoginResult loginStaff(String username, String pin) {
        StaffUser user = staff.findById(username.trim().toLowerCase(Locale.ROOT))
                .filter(u -> PinHasher.matches(u.getPinSalt(), pin, u.getPinHash()))
                .orElseThrow(() -> ApiException.unauthorized("errStaff"));
        activityLog.log(user.getDisplayName(), "Loggade in", "Logged in");
        return start(Session.Role.STAFF, user.getUsername(), user.getDisplayName());
    }

    @Transactional
    public void logout(String token) {
        Session session = sessions.remove(token);
        if (session != null) {
            activityLog.log(session.name(), "Loggade ut", "Logged out");
        }
    }

    /** Looks up a token; expired tokens are thrown away. */
    public Optional<Session> find(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    private LoginResult start(Session.Role role, String subject, String name) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(role, subject, name, Instant.now().plus(properties.sessionTtl())));
        return new LoginResult(token, role, name);
    }
}
