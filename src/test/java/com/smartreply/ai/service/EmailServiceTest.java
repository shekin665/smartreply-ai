package com.smartreply.ai.service;

import com.smartreply.ai.config.DemoDataSeeder;
import com.smartreply.ai.exception.EmailNotFoundException;
import com.smartreply.ai.exception.MailDeliveryException;
import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;
import com.smartreply.ai.model.ReceivedEmail;
import com.smartreply.ai.model.SentEmail;
import com.smartreply.ai.service.mail.InboundMailGateway;
import com.smartreply.ai.service.mail.OutboundMailGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailServiceTest {

    private static final String FROM = "signedin@gmail.com";
    private static final String FROM_NAME = "Signed-In User";
    private static final String TOKEN = "fake-access-token";

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(new DemoDataSeeder(), new NoOpOutboundGateway(), new EmptyInboundGateway());
    }

    @Test
    void seedsAtLeastTwentyEmailsAcrossFolders() {
        long total = 0;
        for (Folder folder : Folder.values()) {
            total += emailService.getByFolder(folder).size();
        }
        assertTrue(total >= 20, "Expected at least 20 seeded demo emails, found " + total);
        assertFalse(emailService.getByFolder(Folder.INBOX).isEmpty());
        assertFalse(emailService.getByFolder(Folder.SENT).isEmpty());
        assertFalse(emailService.getByFolder(Folder.DRAFTS).isEmpty());
        assertFalse(emailService.getByFolder(Folder.SPAM).isEmpty());
    }

    @Test
    void getByIdThrowsWhenEmailDoesNotExist() {
        assertThrows(EmailNotFoundException.class, () -> emailService.getById("does-not-exist"));
    }

    @Test
    void markAsSpamThenRestoreReturnsReceivedEmailToInbox() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        String id = email.getId();

        emailService.markAsSpam(id);
        assertEquals(Folder.SPAM, emailService.getById(id).getFolder());

        emailService.restore(id);
        assertEquals(Folder.INBOX, emailService.getById(id).getFolder());
    }

    @Test
    void toggleStarFlipsStarredFlag() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        boolean before = email.isStarred();

        emailService.toggleStar(email.getId());

        assertEquals(!before, emailService.getById(email.getId()).isStarred());
    }

    @Test
    void deleteIsSoftThenPermanent() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        String id = email.getId();

        emailService.delete(id); // soft delete -> TRASH
        assertEquals(Folder.TRASH, emailService.getById(id).getFolder());

        emailService.delete(id); // second delete from TRASH -> gone for good
        assertThrows(EmailNotFoundException.class, () -> emailService.getById(id));
    }

    @Test
    void saveDraftCreatesUnsentSentEmailInDraftsFolder() {
        SentEmail draft = emailService.saveDraft(FROM, FROM_NAME, null, "someone@example.com", "Hello", "Draft body", null);

        assertEquals(Folder.DRAFTS, draft.getFolder());
        assertFalse(draft.isSent());
        assertEquals("someone@example.com", draft.getToAddress());
        assertEquals(FROM, draft.getFromAddress());
    }

    @Test
    void sendMovesEmailToSentFolderAndMarksItSent() {
        SentEmail email = emailService.send(TOKEN, FROM, FROM_NAME, null, "someone@example.com", "Hi", "body", null, false);

        assertEquals(Folder.SENT, email.getFolder());
        assertTrue(email.isSent());
        assertEquals(Folder.SENT, emailService.getById(email.getId()).getFolder());
    }

    @Test
    void sendWithoutRecipientThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> emailService.send(TOKEN, FROM, FROM_NAME, null, "", "subject", "body", null, false));
    }

    @Test
    void searchFiltersWithinFolderOnly() {
        List<Email> matches = emailService.search(Folder.INBOX, "assignment");
        assertFalse(matches.isEmpty());
        matches.forEach(e -> assertEquals(Folder.INBOX, e.getFolder()));
    }

    @Test
    void resetRestoresOriginalSeedCounts() {
        int before = emailService.getByFolder(Folder.INBOX).size();
        emailService.delete(emailService.getByFolder(Folder.INBOX).get(0).getId());

        emailService.reset();

        assertEquals(before, emailService.getByFolder(Folder.INBOX).size());
    }

    @Test
    void sendDelegatesToOutboundMailGatewayBeforeCommittingState() {
        RecordingGateway gateway = new RecordingGateway();
        EmailService serviceWithRecordingGateway = new EmailService(new DemoDataSeeder(), gateway, new EmptyInboundGateway());

        serviceWithRecordingGateway.send(TOKEN, FROM, FROM_NAME, null, "someone@example.com", "Hi", "body", null, false);

        assertEquals(TOKEN, gateway.lastAccessToken);
        assertEquals("someone@example.com", gateway.lastTo);
        assertEquals("Hi", gateway.lastSubject);
    }

    @Test
    void failedGatewaySendLeavesDraftUntouched() {
        EmailService serviceWithFailingGateway = new EmailService(new DemoDataSeeder(), new FailingGateway(), new EmptyInboundGateway());
        SentEmail draft = serviceWithFailingGateway.saveDraft(FROM, FROM_NAME, null, "someone@example.com", "Hi", "body", null);

        assertThrows(MailDeliveryException.class, () ->
                serviceWithFailingGateway.send(TOKEN, FROM, FROM_NAME, draft.getId(), "someone@example.com", "Hi", "body", null, false));

        // the draft must still be exactly as it was - never marked sent
        SentEmail unchanged = (SentEmail) serviceWithFailingGateway.getById(draft.getId());
        assertFalse(unchanged.isSent());
        assertEquals(Folder.DRAFTS, unchanged.getFolder());
    }

    @Test
    void syncRealInboxImportsNewMessagesAndDedupesOnRepeatSync() {
        EmailService serviceWithFixedInbound = new EmailService(
                new DemoDataSeeder(), new NoOpOutboundGateway(), new FixedInboundGateway());
        int inboxBefore = serviceWithFixedInbound.getByFolder(Folder.INBOX).size();

        int firstImportCount = serviceWithFixedInbound.syncRealInbox(TOKEN, FROM, 20);
        assertEquals(2, firstImportCount);
        assertEquals(inboxBefore + 2, serviceWithFixedInbound.getByFolder(Folder.INBOX).size());
        Email imported = serviceWithFixedInbound.getById("gmail-1");
        assertTrue(imported.isFromRealGmail());

        int secondImportCount = serviceWithFixedInbound.syncRealInbox(TOKEN, FROM, 20); // same two messages again
        assertEquals(0, secondImportCount, "already-imported messages must not be duplicated");
        assertEquals(inboxBefore + 2, serviceWithFixedInbound.getByFolder(Folder.INBOX).size());
    }

    // ---------- Test doubles ----------

    private static class NoOpOutboundGateway implements OutboundMailGateway {
        @Override
        public void send(String accessToken, String fromAddress, String fromName, String to, String subject, String body) {
            // no-op - just proves EmailService doesn't need a real network call to function
        }
    }

    /** Records what EmailService asked it to send. */
    private static class RecordingGateway implements OutboundMailGateway {
        String lastAccessToken;
        String lastTo;
        String lastSubject;

        @Override
        public void send(String accessToken, String fromAddress, String fromName, String to, String subject, String body) {
            this.lastAccessToken = accessToken;
            this.lastTo = to;
            this.lastSubject = subject;
        }
    }

    /** Simulates a real Gmail API failure (bad token, invalid recipient, network error, etc). */
    private static class FailingGateway implements OutboundMailGateway {
        @Override
        public void send(String accessToken, String fromAddress, String fromName, String to, String subject, String body) {
            throw new MailDeliveryException("simulated Gmail API failure", new RuntimeException("boom"));
        }
    }

    private static class EmptyInboundGateway implements InboundMailGateway {
        @Override
        public List<ReceivedEmail> fetchRecentInbox(String accessToken, String currentUserAddress, int limit) {
            return List.of();
        }
    }

    /** Stands in for a real Gmail API fetch, returning the same two fixed messages every time. */
    private static class FixedInboundGateway implements InboundMailGateway {
        @Override
        public List<ReceivedEmail> fetchRecentInbox(String accessToken, String currentUserAddress, int limit) {
            ReceivedEmail one = new ReceivedEmail("gmail-1", "alice@gmail.com", "Alice", currentUserAddress,
                    "Real subject 1", "Real body 1", LocalDateTime.now(), Folder.INBOX);
            ReceivedEmail two = new ReceivedEmail("gmail-2", "bob@gmail.com", "Bob", currentUserAddress,
                    "Real subject 2", "Real body 2", LocalDateTime.now(), Folder.INBOX);
            one.setFromRealGmail(true);
            two.setFromRealGmail(true);
            return List.of(one, two);
        }
    }
}
