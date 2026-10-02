package se.hildur.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Hashes staff PINs so the database never holds the PIN itself.
 * A random salt per user means two users with the same PIN get different hashes.
 * (A production system would use a slow hash such as bcrypt via Spring Security.)
 */
public final class PinHasher {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PinHasher() {
    }

    public static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public static String hash(String salt, String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((salt + ":" + pin).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    /** Constant-time comparison, so response timing doesn't leak how close a guess was. */
    public static boolean matches(String salt, String pin, String expectedHash) {
        return MessageDigest.isEqual(
                hash(salt, pin).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
