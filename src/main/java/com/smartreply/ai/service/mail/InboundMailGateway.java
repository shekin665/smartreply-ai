package com.smartreply.ai.service.mail;

import com.smartreply.ai.model.ReceivedEmail;

import java.util.List;

/**
 * Mirror image of {@link OutboundMailGateway} for reading mail instead of
 * sending it. The one implementation, {@link GmailApiInboundGateway}, calls
 * the real Gmail REST API (read-only - it never writes back to the user's
 * mailbox) using the signed-in user's own OAuth2 access token.
 */
public interface InboundMailGateway {

    /**
     * Fetches up to {@code limit} of the most recent INBOX messages.
     *
     * @param accessToken       the caller's current Google OAuth2 access token (must include
     *                          the {@code gmail.readonly} scope)
     * @param currentUserAddress the signed-in user's own Gmail address (used as the "to" field
     *                           on the returned {@link ReceivedEmail} objects)
     */
    List<ReceivedEmail> fetchRecentInbox(String accessToken, String currentUserAddress, int limit);
}
