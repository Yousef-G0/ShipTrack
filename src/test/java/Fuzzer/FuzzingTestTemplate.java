package Fuzzer;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueMedium;

import org.example.SecurityUtils;
import org.example.ShipTrackSystem;
import org.example.User;
import org.example.RegistrationException;

public class FuzzingTestTemplate {

    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int target = data.consumeInt(1, 6);

        switch (target) {
            case 1: fuzzPasswordPolicy(data); break;
            case 2: fuzzCryptography(data); break;
            case 3: fuzzRegistration(data); break;
            case 4: fuzzLoginAndLockout(data); break;
            case 5: fuzzAuthorization(data); break;
            case 6: fuzzConfidentiality(data); break;
        }
    }

    // Password Policy Fuzzing
    private static void fuzzPasswordPolicy(FuzzedDataProvider data) {
        String fuzzPass = data.consumeString(data.remainingBytes());
        int minLength = Math.max(1, data.consumeInt(1, 128));
        int minUpper = Math.max(0, data.consumeInt(0, 5));
        int minLower = Math.max(0, data.consumeInt(0, 5));
        int minDigit = Math.max(0, data.consumeInt(0, 5));
        int minSpecial = Math.max(0, data.consumeInt(0, 5));

        try {
            boolean isValid = SecurityUtils.isValidPassword(fuzzPass, minLength, minUpper, minLower, minDigit, minSpecial);
            if (isValid) {
                if (fuzzPass.length() < minLength) {
                    throw new FuzzerSecurityIssueMedium("Logic Error: Valid password is shorter than minLength");
                }
                validateCharacterRequirements(fuzzPass, minUpper, minLower, minDigit, minSpecial);
            }
        } catch (FuzzerSecurityIssueLow | FuzzerSecurityIssueMedium e) { throw e;
        } catch (Exception e) { throw new FuzzerSecurityIssueMedium("Password validation crashed"); }
    }

    private static void validateCharacterRequirements(String password, int minUpper, int minLower, int minDigit, int minSpecial) {
        int upperCount = 0, lowerCount = 0, digitCount = 0, specialCount = 0;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) upperCount++;
            else if (Character.isLowerCase(c)) lowerCount++;
            else if (Character.isDigit(c)) digitCount++;
            else specialCount++;
        }
        if (upperCount < minUpper || lowerCount < minLower || digitCount < minDigit || specialCount < minSpecial) {
            throw new FuzzerSecurityIssueMedium("Logic Error: Valid password missing required chars");
        }
    }

    // Cryptography Fuzzing
    private static void fuzzCryptography(FuzzedDataProvider data) {
        String fuzzInput = data.consumeString(data.remainingBytes());
        try {
            byte[] salt1 = SecurityUtils.generateSalt();
            String hash1 = SecurityUtils.hashPassword(fuzzInput, salt1);
            String hash2 = SecurityUtils.hashPassword(fuzzInput, salt1);

            if (!hash1.equals(hash2)) throw new FuzzerSecurityIssueMedium("Cryptography Error: Non-deterministic hashing");
            if (hash1 == null || hash1.length() < 40) throw new FuzzerSecurityIssueMedium("Cryptography Error: Hash too short");

            byte[] salt2 = SecurityUtils.generateSalt();
            String hash3 = SecurityUtils.hashPassword(fuzzInput, salt2);
            if (hash1.equals(hash3) && !java.util.Arrays.equals(salt1, salt2)) throw new FuzzerSecurityIssueMedium("Cryptography Error: Different salts, same hash");

        } catch (FuzzerSecurityIssueLow | FuzzerSecurityIssueMedium e) { throw e;
        } catch (Exception e) { throw new FuzzerSecurityIssueMedium("Cryptography crashed"); }
    }

    // Registration Fuzzing
    private static void fuzzRegistration(FuzzedDataProvider data) {
        ShipTrackSystem system = new ShipTrackSystem();
        system.initializePermissions();

        int remaining = data.remainingBytes();
        String fuzzUser = data.consumeString(remaining > 0 ? remaining / 5 : 0);
        String fuzzName = data.consumeString(remaining > 0 ? remaining / 4 : 0);
        String fuzzId = data.consumeString(remaining > 0 ? remaining / 3 : 0);
        String fuzzContact = data.consumeString(remaining > 0 ? remaining / 2 : 0);
        String fuzzPass = data.consumeString(data.remainingBytes());

        try {
            system.registerUser(fuzzUser, fuzzName, fuzzId, fuzzContact, User.Role.CUSTOMER, fuzzPass);
            User registered = system.login(fuzzUser, fuzzPass);
            if (registered == null) throw new FuzzerSecurityIssueMedium("Registration Error: Registered but can't login");
        } catch (RegistrationException e) {
            // Expected for weak fuzzed passwords
        } catch (FuzzerSecurityIssueLow | FuzzerSecurityIssueMedium e) { throw e;
        } catch (Exception e) { throw new FuzzerSecurityIssueMedium("Registration crashed"); }
    }

    //  Login & Lockout Fuzzing
    private static void fuzzLoginAndLockout(FuzzedDataProvider data) {
        ShipTrackSystem system = new ShipTrackSystem();
        system.initializePermissions();

        // Consume fuzzed data for ALL registration fields
        String fuzzSetupUser = data.consumeString(10);
        String fuzzSetupName = data.consumeString(15);
        String fuzzSetupId = data.consumeString(8);
        String fuzzSetupContact = data.consumeString(10);
        String fuzzSetupPass = data.consumeString(15);

        try {
            // All parameters are now fuzzed
            system.registerUser(fuzzSetupUser, fuzzSetupName, fuzzSetupId, fuzzSetupContact, User.Role.CUSTOMER, fuzzSetupPass);
        } catch (RegistrationException e) {
            return; // Setup failed (likely weak fuzzed password), skip this iteration.
        }

        // Now test login and lockout with remaining fuzz data
        String fuzzLoginAttemptPass = data.consumeString(data.remainingBytes());

        try {
            // Fail 3 times to trigger lockout
            system.login(fuzzSetupUser, fuzzLoginAttemptPass + "_1");
            system.login(fuzzSetupUser, fuzzLoginAttemptPass + "_2");
            system.login(fuzzSetupUser, fuzzLoginAttemptPass + "_3");

            // 4th attempt with the CORRECT fuzzed setup password
            User result = system.login(fuzzSetupUser, fuzzSetupPass);

            // If login succeeds after 3 failed attempts, lockout is broken!
            if (result != null) {
                throw new FuzzerSecurityIssueMedium("Lockout Error: User not locked after 3 failed attempts");
            }

        } catch (FuzzerSecurityIssueLow | FuzzerSecurityIssueMedium e) { throw e;
        } catch (Exception e) { throw new FuzzerSecurityIssueMedium("Login mechanism crashed"); }
    }

    //  Authorization Fuzzing
    private static void fuzzAuthorization(FuzzedDataProvider data) {
        ShipTrackSystem system = new ShipTrackSystem();
        system.initializePermissions();

        String fuzzTarget = data.consumeString(data.remainingBytes() > 0 ? data.remainingBytes() / 2 : 0);
        User.Role[] roles = {User.Role.SYSTEM_ADMIN, User.Role.DISPATCHER, User.Role.CUSTOMER};
        User.Role randomRole = roles[data.consumeInt(0, roles.length - 1)];

        try {
            system.removeStaffUser(fuzzTarget, randomRole);
            system.toggleLock(fuzzTarget, randomRole);
        } catch (Exception e) {
            throw new FuzzerSecurityIssueMedium("Authorization logic crashed");
        }
    }

    //  Confidentiality Fuzzing
    private static void fuzzConfidentiality(FuzzedDataProvider data) {
        ShipTrackSystem system = new ShipTrackSystem();
        system.initializePermissions();

        // Consume fuzzed data for User 1
        String owner1 = data.consumeString(10);
        String name1 = data.consumeString(15);
        String id1 = data.consumeString(8);
        String contact1 = data.consumeString(10);
        String pass1 = data.consumeString(15);

        // Consume fuzzed data for User 2
        String owner2 = data.consumeString(10);
        String name2 = data.consumeString(15);
        String id2 = data.consumeString(8);
        String contact2 = data.consumeString(10);
        String pass2 = data.consumeString(15);

        try {
            // All registration parameters are fuzzed
            system.registerUser(owner1, name1, id1, contact1, User.Role.CUSTOMER, pass1);
            system.registerUser(owner2, name2, id2, contact2, User.Role.CUSTOMER, pass2);
        } catch (RegistrationException e) {
            return; // Setup failed, skip iteration
        }

        // Consume fuzzed data for Shipment Description
        String fuzzDesc = data.consumeString(20);

        // Create a shipment for owner1 with fuzzed description
        String shipmentId = system.createShipment(owner1, fuzzDesc);

        // Try to track it with fuzzed requester
        String fuzzRequester = data.consumeString(data.remainingBytes());

        try {
            String result = system.trackShipment(shipmentId, fuzzRequester);

            // Our system returns "Not Found" for unauthorized users
            if (fuzzRequester.equals(owner1) && result.equals("Not Found")) {
                throw new FuzzerSecurityIssueMedium("Confidentiality Error: Owner cannot access own shipment");
            }

            // If someone other than owner1 gets the actual status (not "Not Found"), that's a breach!
            if (!fuzzRequester.equals(owner1) && !result.equals("Not Found")) {
                throw new FuzzerSecurityIssueMedium("Confidentiality Error: Non-owner accessed shipment data!");
            }

        } catch (FuzzerSecurityIssueLow | FuzzerSecurityIssueMedium e) { throw e;
        } catch (Exception e) { throw new FuzzerSecurityIssueMedium("Confidentiality check crashed"); }
    }
}
