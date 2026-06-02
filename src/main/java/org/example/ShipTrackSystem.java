package org.example;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShipTrackSystem {
    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Shipment> shipments = new HashMap<>();

    // Password Policy Defaults
    private int minChars = 8, minUpper = 1, minLower = 1, minDigits = 1, minSpecial = 1;
    private int maxLoginAttempts = 3;

    // --- MAINTAINABILITY: Each feature has its own clear method ---

    public boolean adminExists() {
        for (User user : users.values()) {
            if (user.getRole() == User.Role.SYSTEM_ADMIN) return true;
        }
        return false;
    }

    // Notice we added "throws RegistrationException" to the method signature
    public User registerUser(String username, String name, String id, String contact, User.Role role, String password) throws RegistrationException {

        // Security Principle: Fail Securely with specific, safe messages
        if (users.containsKey(username)) {
            throw new RegistrationException("Username already exists. Please choose another.");
        }

        if (!SecurityUtils.isValidPassword(password, minChars, minUpper, minLower, minDigits, minSpecial)) {
            AuditLogger.logAction(username, "REGISTER_FAIL_POLICY");


            String policyMsg = "Password Policy: Minimum " + minChars + " chars, " +
                    minUpper + " uppercase, " + minLower + " lowercase, " +
                    minDigits + " digit, " + minSpecial + " special character.";

            throw new RegistrationException("Password does not meet security requirements. " + policyMsg);
        }
        if (users.containsKey(username)) {
            AuditLogger.logAction(username, "REGISTER_FAIL_DUPLICATE"); // Changed
            throw new RegistrationException("Username already exists. Please choose another.");
        }
        // Layer 1: Password Policy Enforcement
        if (!SecurityUtils.isValidPassword(password, minChars, minUpper, minLower, minDigits, minSpecial)) {
            AuditLogger.logAction(username, "REGISTER_FAIL_POLICY"); // Changed
            throw new RegistrationException("Password does not meet the security policy requirements.");
        }
        try {
            User newUser = new User(username, name, id, contact, role, maxLoginAttempts);
            // Layer 2: Cryptographic Salting
            byte[] salt = SecurityUtils.generateSalt();
            newUser.setSalt(salt);
            // Layer 3: SHA-256 Hashing
            newUser.setPasswordHash(SecurityUtils.hashPassword(password, salt));

            users.put(username, newUser);
            saveAuthData();
            saveSensitivePII();
            AuditLogger.logAction(username, "REGISTER_SUCCESS");
            return newUser; // Only returns user object if everything succeeds

        } catch (Exception e) {
            AuditLogger.logError("SYSTEM", "REGISTER_SYSTEM_ERROR", e);
            throw new RegistrationException("System error during registration. Please try again later.");
        }
    }

    public User login(String username, String password) {
        User user = users.get(username);

        if (user == null || user.isLocked()) {
            System.out.println(" Invalid credentials or account locked.");
            AuditLogger.logAction (username, "LOGIN_FAIL_LOCKED");
            return null;
        }

        String attemptHash = SecurityUtils.hashPassword(password, user.getSalt());
        if (user.getPasswordHash().equals(attemptHash)) {
            user.resetFailedAttempts();
            saveAuthData();
            AuditLogger.logAction(username, "LOGIN_SUCCESS");
            return user;
        } else {
            user.incrementFailedAttempts();
            // Layer 4: Account Lockout Mechanism (in login method)
            if (user.getFailedAttempts() >= user.getMaxLoginAttempts()) {
                user.setLocked(true);
                System.out.println(" Account locked due to too many failed attempts.");
                AuditLogger.logAction(username, "ACCOUNT_LOCKED"); // Audit Log
            } else {
                System.out.println("Invalid username or password");
                AuditLogger.logAction(username, "LOGIN_FAIL_PASSWORD"); // Audit Log
            }
            saveAuthData();
            return null;
        }
    }
    public void setPolicy(int chars, int upper, int lower, int digits, int special, int maxAttempts, User.Role requesterRole) {
        // Security: Verify permission before changing policy
        if (!hasPermission(requesterRole, "SET_POLICY")) {
            AuditLogger.logSecurityAlert(requesterRole.name(), "UNAUTHORIZED_POLICY_CHANGE");
            return;
        }
        this.minChars = chars; this.minUpper = upper; this.minLower = lower;
        this.minDigits = digits; this.minSpecial = special; this.maxLoginAttempts = maxAttempts;
        savePolicyData(); // D3: password_policy.csv
        AuditLogger.logAction("SYSTEM", "POLICY_UPDATED"); // Audit Log
    }
    // Security: Re-authentication method for sensitive admin actions
    public boolean verifyPassword(User user, String attemptedPassword) {
        String attemptHash = SecurityUtils.hashPassword(attemptedPassword, user.getSalt());
        return user.getPasswordHash().equals(attemptHash);
    }

    // Security Principle: Least Privilege & Authorization
    // Admin can ONLY remove Dispatchers or Delivery Personnel
    public boolean removeStaffUser(String username, User.Role adminRole) {
        // Security: Check D6 Permission Matrix before doing anything!
        if (!hasPermission(adminRole, "REMOVE_STAFF")) {
            AuditLogger.logAction("SYSTEM", "UNAUTHORIZED_REMOVE_ATTEMPT");
            return false;
        }

        User user = users.get(username);
        if (user != null && (user.getRole() == User.Role.DISPATCHER || user.getRole() == User.Role.DELIVERY_PERSONNEL)) {
            users.remove(username);
            saveAuthData();
            saveSensitivePII();
            AuditLogger.logAction(username, "STAFF_REMOVED");
            return true;
        }
        return false;
    }

    public void toggleLock(String username, User.Role requesterRole) {
        // Security: Verify permission before locking/unlocking
        if (!hasPermission(requesterRole, "LOCK_UNLOCK")) {
            AuditLogger.logSecurityAlert(requesterRole.name(), "UNAUTHORIZED_LOCK_ATTEMPT");
            return;
        }

        User user = users.get(username);
        if (user != null) {
            user.setLocked(!user.isLocked());
            saveAuthData();
            AuditLogger.logAction(username, user.isLocked() ? "ACCOUNT_LOCKED" : "ACCOUNT_UNLOCKED"); // Audit Log
        }
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public String getUserInfo(String username) {
        User u = users.get(username);
        if (u == null) return "User not found.";
        return "Username: " + u.getUsername()
                + " | Role: " + u.getRole()
                + " | Locked: " + (u.isLocked() ? "YES " : "NO ")
                + " | Failed Attempts: " + u.getFailedAttempts() + "/" + u.getMaxLoginAttempts();
    }


    public boolean assignDelivery(String shipmentId, String driverUsername, User.Role requesterRole) {
        // Security: Verify permission before assigning
        if (!hasPermission(requesterRole, "ASSIGN_DELIVERY")) {
            AuditLogger.logSecurityAlert(requesterRole.name(), "UNAUTHORIZED_ASSIGN_ATTEMPT");
            return false;
        }

        Shipment shipment = shipments.get(shipmentId);
        if (shipment != null && users.containsKey(driverUsername) && users.get(driverUsername).getRole() == User.Role.DELIVERY_PERSONNEL) {
            shipment.setAssignedDriverId(driverUsername);
            shipment.setStatus("in transit");
            saveShipmentData();
            return true;
        }
        return false;
    }

    public String createShipment(String customerId, String desc) {
        String id = "SHP-" + UUID.randomUUID().toString().substring(0, 6);
        shipments.put(id, new Shipment(id, customerId, desc));
        saveShipmentData();
        return id;
    }

    // Customer: Track shipment (With Confidentiality Check)
    public String trackShipment(String shipmentId, String requesterUsername) {
        Shipment shipment = shipments.get(shipmentId);

        // Security Fix: Least Privilege & Confidentiality
        // Check if the shipment exists AND belongs to the person asking
        if (shipment != null && shipment.getCustomerId().equals(requesterUsername)) {
            return shipment.getStatus();
        }

        // Fail Securely: If it's not theirs, or it doesn't exist, just say "Not Found"
        // Don't say "Access Denied" because that confirms the ID exists to attackers
        return "Not Found";
    }

    public boolean updateDeliveryStatus(String shipmentId, String newStatus, User.Role requesterRole) {
        // Security: Verify permission before driver updates status
        if (!hasPermission(requesterRole, "UPDATE_DELIVERY_STATUS")) {
            AuditLogger.logSecurityAlert(requesterRole.name(), "UNAUTHORIZED_STATUS_UPDATE");
            return false;
        }

        Shipment shipment = shipments.get(shipmentId);
        if (shipment != null && (newStatus.equals("picked up") || newStatus.equals("in transit") || newStatus.equals("delivered"))) {
            shipment.setStatus(newStatus);
            saveShipmentData();
            return true;
        }
        return false;
    }

    public boolean updateDeliveryStatusByDispatcher(String shipmentId, String newStatus, User.Role requesterRole) {
        // Security: Verify permission before dispatcher updates status
        if (!hasPermission(requesterRole, "UPDATE_DISPATCH_STATUS")) {
            AuditLogger.logSecurityAlert(requesterRole.name(), "UNAUTHORIZED_STATUS_UPDATE");
            return false;
        }

        Shipment shipment = shipments.get(shipmentId);
        if (shipment != null && (newStatus.equals("pending") || newStatus.equals("in transit") || newStatus.equals("delivered"))) {
            shipment.setStatus(newStatus);
            saveShipmentData();
            return true;
        }
        return false;
    }

    public void updateUserInfo(String username, String newName, String newContact) {
        User user = users.get(username);
        if (user != null) {
            if (newName != null) user.setName(newName);
            if (newContact != null) user.setContactNumber(newContact);
            saveSensitivePII(); // D5: Update PII file
        }
    }

    public String getDriverAssignments(String driverUsername) {
        StringBuilder result = new StringBuilder();
        for (Shipment shipment : shipments.values()) {
            if (driverUsername.equals(shipment.getAssignedDriverId())) {
                result.append("  ID: ").append(shipment.getShipmentId())
                        .append(" | Status: ").append(shipment.getStatus())
                        .append(" | Desc: ").append(shipment.getDescription()).append("\n");
            }
        }
        return result.length() == 0 ? "No assignments yet." : result.toString();
    }
    // D3: password_policy.csv (With History)
    public void savePolicyData() {
        String oldHistory = ""; // Simple String to hold old policies
        File file = new File("password_policy.csv");

        // 1. Read existing history and change "new" to "old"
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("new,")) {
                        oldHistory += line.replaceFirst("new,", "old,") + "\n"; // Change newest to old
                    } else {
                        oldHistory += line + "\n"; // Keep existing old lines
                    }
                }
            } catch (IOException e) {
                AuditLogger.logError("SYSTEM", "READ_POLICY_HISTORY_FAILED", e);
            }
        }

        // 2. Write the updated history (Newest at the top)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("password_policy.csv"))) {
            // Write the brand new policy first
            String timestamp = LocalDateTime.now().toString();
            writer.write("new," + minChars + "," + minUpper + "," + minLower + "," + minDigits + "," + minSpecial + "," + maxLoginAttempts + "," + timestamp);
            writer.newLine();

            // Write all the old policies below it
            if (!oldHistory.isEmpty()) {
                writer.write(oldHistory);
            }
        } catch (IOException e) {
            System.out.println(" Error saving policy history.");
            AuditLogger.logError("SYSTEM", "SAVE_POLICY_HISTORY_FAILED", e);
        }
    }

    public void loadPolicyData() {
        File file = new File("password_policy.csv");
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Security: Only enforce the policy marked as "new"
                if (line.startsWith("new,")) {
                    String[] p = line.split(",");
                    minChars = Integer.parseInt(p[1]);
                    minUpper = Integer.parseInt(p[2]);
                    minLower = Integer.parseInt(p[3]);
                    minDigits = Integer.parseInt(p[4]);
                    minSpecial = Integer.parseInt(p[5]);
                    maxLoginAttempts = Integer.parseInt(p[6]);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(" Error loading policy. Using defaults.");
            AuditLogger.logError("SYSTEM", "LOAD_POLICY_FAILED", e);
        }
    }

    // D1: users.csv (Authentication & Authorization Only)
    public void saveAuthData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.csv"))) {
            for (User user : users.values()) {
                String saltString = Base64.getEncoder().encodeToString(user.getSalt());
                String line = user.getUsername() + "," + user.getRole() + "," +
                        user.getPasswordHash() + "," + saltString + "," +
                        user.isLocked() + "," + user.getFailedAttempts() + "," + user.getMaxLoginAttempts();
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println(" Error saving auth data.");
            AuditLogger.logError("SYSTEM", "SAVE_AUTH_DATA_FAILED", e);
        }
    }

    // D5: sensitive_pii.csv (Data Segregation Principle)
    public void saveSensitivePII() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("sensitive_pii.csv"))) {
            for (User user : users.values()) {
                String line = user.getUsername() + "," + user.getName() + "," +
                        user.getIdNumber() + "," + user.getContactNumber();
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println(" Error saving PII data.");
        }
    }

    // Load and Merge D1 + D5 back into User Objects
    public void loadUsers() {
        // Step 1: Load Auth Data (D1)
        File authFile = new File("users.csv");
        if (authFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(authFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] p = line.split(",", -1);
                    User user = new User(p[0], "", "", "", User.Role.valueOf(p[1]), Integer.parseInt(p[6]));
                    user.setPasswordHash(p[2]);
                    user.setSalt(Base64.getDecoder().decode(p[3]));
                    user.setLocked(Boolean.parseBoolean(p[4]));
                    user.setFailedAttempts(Integer.parseInt(p[5]));
                    users.put(p[0], user);
                }
            } catch (Exception e) {
                System.out.println(" Error loading auth data.");
            }
        }

        // Step 2: Load PII Data (D5) and Merge
        File piiFile = new File("sensitive_pii.csv");
        if (piiFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(piiFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] p = line.split(",", -1);
                    User user = users.get(p[0]);
                    if (user != null) {
                        user.setName(p[1]);
                        user.setIdNumber(p[2]);
                        user.setContactNumber(p[3]);
                    }
                }
            } catch (Exception e) {
                System.out.println(" Error loading PII data.");
            }
        }
    }

    // D2: shipments.csv
    public void saveShipmentData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("shipments.csv"))) {
            for (Shipment shipment : shipments.values()) {
                String driverId = (shipment.getAssignedDriverId() == null) ? "null" : shipment.getAssignedDriverId();
                String line = shipment.getShipmentId() + "," + shipment.getCustomerId() + "," +
                        driverId + "," + shipment.getStatus() + "," + shipment.getDescription().replace(",", ";");
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println(" Error saving shipments.");
        }
    }

    public void loadShipments() {
        File file = new File("shipments.csv");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                Shipment shipment = new Shipment(p[0], p[1], p[4]);
                shipment.setAssignedDriverId(p[2].equals("null") ? null : p[2]);
                shipment.setStatus(p[3]);
                shipments.put(p[0], shipment);
            }
        } catch (Exception e) {
            System.out.println(" Error loading shipments.");
        }
    }
    // D6: role_permissions.csv (Access Control Matrix)
    public void initializePermissions() {
        File file = new File("role_permissions.csv");
        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Define least privilege permissions per role
                writer.write("CUSTOMER,CREATE_SHIPMENT,TRACK_SHIPMENT,UPDATE_OWN_INFO\n");
                writer.write("DISPATCHER,ASSIGN_DELIVERY,UPDATE_DISPATCH_STATUS,REGISTER_PERSONNEL,UPDATE_OWN_INFO\n");
                writer.write("DELIVERY_PERSONNEL,VIEW_ASSIGNMENTS,UPDATE_DELIVERY_STATUS\n");
                writer.write("SYSTEM_ADMIN,REGISTER_STAFF,REMOVE_STAFF,LOCK_UNLOCK,SET_POLICY,VIEW_ALL_DATA\n");
            } catch (IOException e) {
                AuditLogger.logSecurityAlert("SYSTEM", "INIT_PERMISSIONS_FAILED");
            }
        }
    }

    // Check if a role has a specific permission
    public boolean hasPermission(User.Role role, String requiredPermission) {
        try (BufferedReader reader = new BufferedReader(new FileReader("role_permissions.csv"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String fileRole = parts[0].trim();

                if (fileRole.equals(role.name())) {
                    for (int i = 1; i < parts.length; i++) {
                        if (parts[i].trim().equals(requiredPermission)) {
                            return true; // Permission granted
                        }
                    }
                }
            }
        } catch (Exception e) {
            AuditLogger.logSecurityAlert("SYSTEM", "PERMISSION_CHECK_FAILED");
        }
        return false; // Deny by default (Fail Securely)
    }
}