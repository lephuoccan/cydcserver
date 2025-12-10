package cloud.cydc.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing and verification utilities using bcrypt.
 */
public class PasswordHasher {
    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hashes a password using bcrypt.
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verifies a plain-text password against a bcrypt hash.
     */
    public static boolean verifyPassword(String plainText, String hash) {
        try {
            return BCrypt.checkpw(plainText, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
