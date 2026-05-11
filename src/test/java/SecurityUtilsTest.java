//package java;

import org.example.SecurityUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityUtilsTest {

    // ==========================================
    // PASSWORD POLICY TESTS
    // ==========================================

    @Test
    public void testStrongPasswordValid() {
        assertTrue(SecurityUtils.isValidPassword("Admin@123", 8, 1, 1, 1, 1),
                "Password meeting all criteria should be valid");
    }

    @Test
    public void testPasswordTooShort() {
        assertFalse(SecurityUtils.isValidPassword("Ad@1", 8, 1, 1, 1, 1),
                "Password less than minChars should be invalid");
    }

    @Test
    public void testPasswordMissingUppercase() {
        assertFalse(SecurityUtils.isValidPassword("admin@123", 8, 1, 1, 1, 1),
                "Password without uppercase should be invalid");
    }

    @Test
    public void testPasswordMissingLowercase() {
        assertFalse(SecurityUtils.isValidPassword("ADMIN@123", 8, 1, 1, 1, 1),
                "Password without lowercase should be invalid");
    }

    @Test
    public void testPasswordMissingDigit() {
        assertFalse(SecurityUtils.isValidPassword("Admin@@@", 8, 1, 1, 1, 1),
                "Password without digit should be invalid");
    }

    @Test
    public void testPasswordMissingSpecial() {
        assertFalse(SecurityUtils.isValidPassword("Admin123", 8, 1, 1, 1, 1),
                "Password without special character should be invalid");
    }

    @Test
    public void testNullPassword() {
        assertFalse(SecurityUtils.isValidPassword(null, 8, 1, 1, 1, 1),
                "Null password should be invalid");
    }

    // ==========================================
    // CRYPTOGRAPHY TESTS
    // ==========================================

    @Test
    public void testHashPasswordConsistency() {
        String password = "SecurePass@1";
        byte[] salt = SecurityUtils.generateSalt();

        String hash1 = SecurityUtils.hashPassword(password, salt);
        String hash2 = SecurityUtils.hashPassword(password, salt);

        assertEquals(hash1, hash2, "Same password and salt must produce the same hash");
    }

    @Test
    public void testHashPasswordDifferentSalts() {
        String password = "SecurePass@1";
        byte[] salt1 = SecurityUtils.generateSalt();
        byte[] salt2 = SecurityUtils.generateSalt();

        String hash1 = SecurityUtils.hashPassword(password, salt1);
        String hash2 = SecurityUtils.hashPassword(password, salt2);

        assertNotEquals(hash1, hash2, "Same password with different salts must produce different hashes");
    }
}