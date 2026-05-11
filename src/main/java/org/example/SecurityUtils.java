package org.example;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    // Generates a cryptographically secure random salt
    public static byte[] generateSalt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }

    // SHA-256 hashing with salt (Cryptographic Standard per assignment)
    public static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedBytes = md.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception e) {
            // Fail Securely: Never expose internal error details
            throw new RuntimeException("Password hashing failed due to security policy.");
        }
    }

    // Validates password against admin-defined policy
    public static boolean isValidPassword(String password, int minChars, int minUpper, int minLower, int minDigits, int minSpecial) {
        if (password == null || password.length() < minChars) return false;

        int upper = 0, lower = 0, digits = 0, special = 0;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) upper++;
            else if (Character.isLowerCase(c)) lower++;
            else if (Character.isDigit(c)) digits++;
            else if (!Character.isLetterOrDigit(c)) special++;
        }
        return upper >= minUpper && lower >= minLower && digits >= minDigits && special >= minSpecial;
    }
}