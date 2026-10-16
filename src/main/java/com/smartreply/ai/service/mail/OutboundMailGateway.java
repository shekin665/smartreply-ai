package com.smartreply.ai.service.mail;

/**
 * OOP concept - Abstraction/Polymorphism: {@link com.smartreply.ai.service.EmailService}
 * only ever talks to this interface when "sending" an email - it never
 * builds an HTTP request or a MIME message itself. The one implementation,
 * {@link GmailApiMailGateway}, calls the real Gmail REST API using the
 * signed-in user's own OAuth2 access token (passed in per call, since a
 * different request can come from a different logged-in user).
 */
public interface OutboundMailGateway {

    /**
     * @param accessToken the caller's current Google OAuth2 access token (must include the
     *                     {@code gmail.send} scope)
     * @param fromAddress the signed-in user's own Gmail address
     * @param fromName    the signed-in user's display name
     */
    void send(String accessToken, String fromAddress, String fromName, String to, String subject, String body);
}
