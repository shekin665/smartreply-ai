package com.smartreply.ai.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartreply.ai.exception.MailSyncException;
import com.smartreply.ai.model.Folder;
import com.smartreply.ai.model.ReceivedEmail;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Reads the real Gmail INBOX through the Gmail REST API
 * ({@code users.messages.list} + {@code users.messages.get}) using the
 * signed-in user's own OAuth2 access token. Strictly read-only - this class
 * never calls any Gmail endpoint that modifies a label, read-state, or
 * message, so a sync can never change anything on the user's real account.
 */
@Component
public class GmailApiInboundGateway implements InboundMailGateway {

    private static final String API_BASE = "https://gmail.googleapis.com/gmail/v1/users/me";
    private static final int MAX_BODY_LENGTH = 5000;

    private final RestClient restClient;

    public GmailApiInboundGateway(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public List<ReceivedEmail> fetchRecentInbox(String accessToken, String currentUserAddress, int limit) {
        try {
            JsonNode listResponse = restClient.get()
                    .uri(API_BASE + "/messages?maxResults={limit}&labelIds=INBOX", limit)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            List<ReceivedEmail> result = new ArrayList<>();
            if (listResponse == null || !listResponse.has("messages")) {
                return result; // empty inbox - nothing to import
            }

            for (JsonNode ref : listResponse.get("messages")) {
                String id = ref.get("id").asText();
                JsonNode full = restClient.get()
                        .uri(API_BASE + "/messages/{id}?format=full", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(JsonNode.class);
                result.add(toReceivedEmail(full, currentUserAddress));
            }
            return result;
        } catch (RestClientResponseException ex) {
            throw new MailSyncException(
                    "Gmail API rejected the inbox fetch: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new MailSyncException("Failed to fetch real Gmail inbox: " + ex.getMessage(), ex);
        }
    }

    private ReceivedEmail toReceivedEmail(JsonNode message, String currentUserAddress) {
        String id = "gmail-" + message.get("id").asText();
        JsonNode headers = message.path("payload").path("headers");

        String subject = headerValue(headers, "Subject", "(no subject)");
        String fromHeader = headerValue(headers, "From", "unknown@gmail.com");

        String fromAddress = fromHeader;
        String fromName = fromHeader;
        try {
            InternetAddress[] parsed = InternetAddress.parse(fromHeader);
            if (parsed.length > 0) {
                fromAddress = parsed[0].getAddress();
                fromName = parsed[0].getPersonal() != null ? parsed[0].getPersonal() : fromAddress;
            }
        } catch (AddressException ignored) {
            // keep the raw header text as a fallback - still better than failing the whole sync
        }

        LocalDateTime date = message.has("internalDate")
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(message.get("internalDate").asLong()), ZoneId.systemDefault())
                : LocalDateTime.now();

        String body = extractBody(message.path("payload"));
        if (body.isBlank() && message.has("snippet")) {
            body = message.get("snippet").asText();
        }

        ReceivedEmail email = new ReceivedEmail(id, fromAddress, fromName, currentUserAddress, subject, body, date, Folder.INBOX);
        email.setFromRealGmail(true);

        boolean unread = false;
        boolean starred = false;
        if (message.has("labelIds")) {
            for (JsonNode label : message.get("labelIds")) {
                if ("UNREAD".equals(label.asText())) unread = true;
                if ("STARRED".equals(label.asText())) starred = true;
            }
        }
        email.setRead(!unread);
        email.setStarred(starred);
        return email;
    }

    private String headerValue(JsonNode headers, String name, String fallback) {
        if (headers.isMissingNode()) {
            return fallback;
        }
        for (JsonNode header : headers) {
            if (name.equalsIgnoreCase(header.path("name").asText())) {
                return header.path("value").asText(fallback);
            }
        }
        return fallback;
    }

    /** Walks the (possibly nested multipart) MIME structure Gmail returns, preferring text/plain. */
    private String extractBody(JsonNode payload) {
        if (payload.isMissingNode()) {
            return "";
        }
        String mimeType = payload.path("mimeType").asText("");

        if (mimeType.startsWith("text/plain")) {
            return decode(payload.path("body").path("data").asText(""));
        }
        if (payload.has("parts")) {
            for (JsonNode part : payload.get("parts")) {
                if (part.path("mimeType").asText("").startsWith("text/plain")) {
                    String text = extractBody(part);
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
            for (JsonNode part : payload.get("parts")) {
                if (part.path("mimeType").asText("").startsWith("text/html")) {
                    return stripHtml(decode(part.path("body").path("data").asText("")));
                }
            }
            for (JsonNode part : payload.get("parts")) {
                String nested = extractBody(part); // nested multipart/alternative, etc.
                if (!nested.isBlank()) {
                    return nested;
                }
            }
        }
        if (mimeType.startsWith("text/html")) {
            return stripHtml(decode(payload.path("body").path("data").asText("")));
        }
        return "";
    }

    private String decode(String base64url) {
        if (base64url == null || base64url.isBlank()) {
            return "";
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64url);
            String text = new String(bytes, StandardCharsets.UTF_8);
            return text.length() > MAX_BODY_LENGTH ? text.substring(0, MAX_BODY_LENGTH) + "…" : text;
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private String stripHtml(String html) {
        return html.replaceAll("(?is)<(script|style).*?</\\1>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
