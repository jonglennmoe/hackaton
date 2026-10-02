package se.hildur.auth;

import java.time.Instant;

/**
 * Who is logged in behind a token.
 *
 * @param role      GUEST or STAFF – decides which half of the API the token opens
 * @param subject   booking number (guest) or username (staff)
 * @param name      display name used in the activity log, e.g. "Anna L. (12)" or "Lisa B."
 * @param expiresAt when the token stops working
 */
public record Session(Role role, String subject, String name, Instant expiresAt) {

    public enum Role { GUEST, STAFF }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
