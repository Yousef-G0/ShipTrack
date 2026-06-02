package org.example;

public class User {
    public enum Role {
        CUSTOMER, DISPATCHER, DELIVERY_PERSONNEL, SYSTEM_ADMIN
    }

    private String username;
    private String name;
    private String idNumber;
    private String contactNumber;
    private Role role;
    private String passwordHash;
    private byte[] salt;
    private boolean isLocked;
    private int failedAttempts;
    private int maxLoginAttempts;
    private long lastSudoTime = 0;

    public User(String username, String name, String idNumber, String contactNumber, Role role, int maxAttempts) {
        this.username = username;
        this.name = name;
        this.idNumber = idNumber;
        this.contactNumber = contactNumber;
        this.role = role;
        this.isLocked = false;
        this.failedAttempts = 0;
        this.maxLoginAttempts = maxAttempts;
    }

    // Getters & Setters
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; } // <-- ADDED THIS
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public byte[] getSalt() { return salt; }
    public void setSalt(byte[] salt) { this.salt = salt; }
    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }
    public int getFailedAttempts() { return failedAttempts; }
    public void incrementFailedAttempts() { this.failedAttempts++; }
    public void resetFailedAttempts() { this.failedAttempts = 0; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int max) { this.maxLoginAttempts = max; }
    public long getLastSudoTime() { return lastSudoTime; }
    public void setLastSudoTime(long lastSudoTime) { this.lastSudoTime = lastSudoTime; }

}