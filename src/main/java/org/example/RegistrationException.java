package org.example;

public class RegistrationException extends Exception {
    public RegistrationException(String message) {
        super(message);
    }
} // stops Registration Immediately if anything wrong happen.
