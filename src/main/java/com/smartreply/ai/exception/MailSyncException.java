package com.smartreply.ai.exception;

/** Thrown when fetching the real inbox over IMAP fails (bad credentials, network error, etc). */
public class MailSyncException extends RuntimeException {

    public MailSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
