package com.smartreply.ai.exception;

/** The text assistant could not run: not set up (503) or the AI service failed (502). */
public class TextAssistException extends RuntimeException {

    private final boolean notConfigured;

    public TextAssistException(String message, boolean notConfigured, Throwable cause) {
        super(message, cause);
        this.notConfigured = notConfigured;
    }

    public boolean isNotConfigured() {
        return notConfigured;
    }
}
