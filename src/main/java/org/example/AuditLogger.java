package org.example;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AuditLogger {
    // Create a logger instance specifically for ShipTrack
    final static Logger LOGGER = Logger.getLogger("ShipTrackAudit");

    static {
        try {
            // Initialize the file handler to write to D4: audit_log.log
            // 'true' means it will APPEND to the file, not overwrite it
            FileHandler fh = new FileHandler("audit_log.log", true);
            LOGGER.addHandler(fh);

            // Use standard formatting for readability
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // SECURITY: Prevent logs from also printing to the console (Attack Surface Reduction)
            // We don't want hackers reading stack traces on the screen if an error happens
            LOGGER.setUseParentHandlers(false);

        } catch (IOException | SecurityException e) {
            // Fail Securely: If the logger breaks, print a warning, but don't crash the app
            System.out.println(" Audit logger initialization failed.");
        }
    }

    // 1. Log standard user actions (INFO level)
    public static void logAction(String username, String action) {
        LOGGER.log(Level.INFO, "User: " + username + " | Action: " + action);
    }

    // 2. Log technical exceptions with full stack traces (WARNING level)
    public static void logError(String username, String action, Exception e) {
        LOGGER.log(Level.WARNING, "User: " + username + " | Action: " + action, e);
    }

    // 3. Log critical security events like account lockouts (SEVERE level)
    public static void logSecurityAlert(String username, String action) {
        LOGGER.log(Level.SEVERE, "SECURITY ALERT | User: " + username + " | Action: " + action);
    }
}