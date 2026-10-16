package com.smartreply.ai.exception;

/**
 * Thrown by a live {@code OutboundMailGateway} when a real SMTP send fails
 * (bad credentials, invalid recipient, network error, etc). EmailService
 * deliberately throws this *before* touching the in-memory mailbox, so a
 * failed real send never leaves a draft in a half-sent state.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
