package com.smartreply.ai.service.mail;

import com.smartreply.ai.exception.MailDeliveryException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

/**
 * Sends real email through the Gmail REST API ({@code users.messages.send}),
 * authenticated with the signed-in user's own OAuth2 access token - no App
 * Password, no SMTP connection, no shared account. {@code jakarta.mail}'s
 * {@link MimeMessage} is used purely as a local RFC 2822 message *builder*
 * here (no network connection is ever opened by it) - the finished message
 * is base64url-encoded and POSTed to Gmail's HTTPS API.
 */
@Component
public class GmailApiMailGateway implements OutboundMailGateway {

    private static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final RestClient restClient;

    public GmailApiMailGateway(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public void send(String accessToken, String fromAddress, String fromName, String to, String subject, String body) {
        try {
            String raw = buildRawMessage(fromAddress, fromName, to, subject, body);
            restClient.post()
                    .uri(SEND_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("raw", raw))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new MailDeliveryException(
                    "Gmail API rejected the send request: " + ex.getStatusCode() + " " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new MailDeliveryException("Failed to send email via Gmail API: " + ex.getMessage(), ex);
        }
    }

    private String buildRawMessage(String fromAddress, String fromName, String to, String subject, String body) throws Exception {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromAddress, fromName));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject, "UTF-8");
        message.setText(body == null ? "" : body, "UTF-8");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        message.writeTo(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.toByteArray());
    }
}
