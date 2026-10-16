package com.smartreply.ai.exception;

/** Thrown when a lookup by email id finds nothing in EmailService. */
public class EmailNotFoundException extends RuntimeException {

    public EmailNotFoundException(String emailId) {
        super("No email found with id: " + emailId);
    }
}
