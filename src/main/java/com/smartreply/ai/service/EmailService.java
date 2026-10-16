package com.smartreply.ai.service;

import com.smartreply.ai.config.DemoDataSeeder;
import com.smartreply.ai.exception.EmailNotFoundException;
import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;
import com.smartreply.ai.model.ReceivedEmail;
import com.smartreply.ai.model.SentEmail;
import com.smartreply.ai.service.mail.InboundMailGateway;
import com.smartreply.ai.service.mail.OutboundMailGateway;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Owns the single in-memory mailbox (an {@link ArrayList} of {@link Email})
 * and every folder/flag operation on it: listing, searching, marking
 * read/unread, starring, archiving, spam/restore, trash/delete, drafts and
 * the real Gmail send/sync. Demonstrates encapsulation (the list is private
 * and only ever touched through these methods) and use of Java collections.
 * <p>
 * The demo/fictional mailbox seeded by {@link DemoDataSeeder} is always
 * present (useful for a viva walkthrough without touching any real account);
 * real messages pulled via {@link #syncRealInbox} are simply merged in
 * alongside it, tagged {@code fromRealGmail=true}.
 * <p>
 * {@code @SessionScope} is important here, not just an optimization: every
 * signed-in user now has their own real Gmail OAuth token, so without
 * per-session isolation a plain singleton would mean ALL logged-in users
 * share one mailbox - one person's synced Gmail messages (and sent/draft
 * emails) would be visible to everyone else too. Spring creates one
 * {@code EmailService} instance per HTTP session instead, matching the
 * mental model of "each signed-in user sees only their own mailbox".
 */
@Service
@SessionScope
public class EmailService {

    private final DemoDataSeeder seeder;
    private final OutboundMailGateway outboundMailGateway;
    private final InboundMailGateway inboundMailGateway;
    private final AtomicInteger idSequence = new AtomicInteger();
    private List<Email> emails;

    public EmailService(DemoDataSeeder seeder, OutboundMailGateway outboundMailGateway,
                         InboundMailGateway inboundMailGateway) {
        this.seeder = seeder;
        this.outboundMailGateway = outboundMailGateway;
        this.inboundMailGateway = inboundMailGateway;
        reset();
    }

    /**
     * Pulls the most recent real Gmail INBOX messages (read-only, via the
     * Gmail REST API) using the caller's own OAuth2 access token, and merges
     * any not already imported into the in-memory mailbox. Returns how many
     * were newly added. De-duplicates by Gmail's own message id, so calling
     * this repeatedly never creates duplicates.
     */
    public synchronized int syncRealInbox(String accessToken, String currentUserAddress, int limit) {
        List<ReceivedEmail> fetched = inboundMailGateway.fetchRecentInbox(accessToken, currentUserAddress, limit);
        if (fetched.isEmpty()) {
            return 0;
        }
        Set<String> existingIds = emails.stream().map(Email::getId).collect(Collectors.toSet());
        int imported = 0;
        for (ReceivedEmail candidate : fetched) {
            if (existingIds.contains(candidate.getId())) {
                continue;
            }
            emails.add(candidate);
            imported++;
        }
        return imported;
    }

    /** Re-seeds the mailbox from scratch (demo content only) so the demo can always be replayed. */
    public synchronized void reset() {
        this.emails = new ArrayList<>(seeder.seedEmails());
        this.idSequence.set(this.emails.size());
    }

    public List<Email> getByFolder(Folder folder) {
        return emails.stream()
                .filter(e -> e.getFolder() == folder)
                .sorted(Comparator.comparing(Email::getDate).reversed())
                .collect(Collectors.toList());
    }

    public List<Email> search(Folder folder, String query) {
        if (query == null || query.isBlank()) {
            return getByFolder(folder);
        }
        String needle = query.trim().toLowerCase();
        return emails.stream()
                .filter(e -> e.getFolder() == folder)
                .filter(e -> e.getSubject().toLowerCase().contains(needle)
                        || e.getBody().toLowerCase().contains(needle)
                        || e.getDisplayName().toLowerCase().contains(needle))
                .sorted(Comparator.comparing(Email::getDate).reversed())
                .collect(Collectors.toList());
    }

    public Email getById(String id) {
        return emails.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new EmailNotFoundException(id));
    }

    public void setRead(String id, boolean read) {
        getById(id).setRead(read);
    }

    public void toggleStar(String id) {
        Email email = getById(id);
        email.setStarred(!email.isStarred());
    }

    public void archive(String id) {
        getById(id).setFolder(Folder.ARCHIVE);
    }

    public void markAsSpam(String id) {
        getById(id).setFolder(Folder.SPAM);
    }

    /** Sends an email "home": SPAM/ARCHIVE/TRASH -> INBOX, or SENT/DRAFTS as appropriate. */
    public void restore(String id) {
        Email email = getById(id);
        email.setFolder(email.homeFolder());
    }

    /** Soft-deletes into TRASH first; a second delete from TRASH (or from DRAFTS) removes it for good. */
    public void delete(String id) {
        Email email = getById(id);
        if (email.getFolder() == Folder.TRASH || email.getFolder() == Folder.DRAFTS) {
            emails.remove(email);
        } else {
            email.setFolder(Folder.TRASH);
        }
    }

    public SentEmail saveDraft(String fromAddress, String fromName, String existingId, String to,
                                String subject, String body, String repliedToEmailId) {
        if (existingId != null) {
            SentEmail draft = requireSentEmail(existingId);
            draft.setToAddress(to);
            draft.setSubject(subject);
            draft.setBody(body);
            draft.setDate(LocalDateTime.now());
            draft.setFolder(Folder.DRAFTS);
            draft.setSent(false);
            return draft;
        }
        SentEmail draft = new SentEmail(nextId(), fromAddress, fromName, to,
                subject, body, LocalDateTime.now(), Folder.DRAFTS, false);
        if (repliedToEmailId != null) {
            draft.setRepliedToEmailId(repliedToEmailId);
            draft.setGeneratedByAi(true);
        }
        emails.add(draft);
        return draft;
    }

    /**
     * Sends a real email through the Gmail REST API, authenticated as the
     * currently signed-in user. The gateway call happens BEFORE any
     * in-memory state changes, so a failed send (bad recipient, revoked
     * token, etc) never leaves a draft half-sent.
     */
    public SentEmail send(String accessToken, String fromAddress, String fromName, String existingId,
                           String to, String subject, String body, String repliedToEmailId, boolean generatedByAi) {
        if (to == null || to.isBlank() || subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Recipient and subject are required to send an email.");
        }
        outboundMailGateway.send(accessToken, fromAddress, fromName, to, subject, body);

        SentEmail email;
        if (existingId != null) {
            email = requireSentEmail(existingId);
            email.setToAddress(to);
            email.setSubject(subject);
            email.setBody(body);
        } else {
            email = new SentEmail(nextId(), fromAddress, fromName, to,
                    subject, body, LocalDateTime.now(), Folder.SENT, true);
            emails.add(email);
        }
        email.setFolder(Folder.SENT);
        email.setSent(true);
        email.setDate(LocalDateTime.now());
        email.setGeneratedByAi(generatedByAi);
        if (repliedToEmailId != null) {
            email.setRepliedToEmailId(repliedToEmailId);
        }
        return email;
    }

    public long countUnread(Folder folder) {
        return emails.stream().filter(e -> e.getFolder() == folder && !e.isRead()).count();
    }

    public Map<Folder, Long> getFolderCounts() {
        Map<Folder, Long> counts = new EnumMap<>(Folder.class);
        for (Folder folder : Folder.values()) {
            counts.put(folder, emails.stream().filter(e -> e.getFolder() == folder).count());
        }
        return counts;
    }

    private SentEmail requireSentEmail(String id) {
        Email email = getById(id);
        if (!(email instanceof SentEmail)) {
            throw new IllegalArgumentException("Only drafts or sent emails authored by the user can be edited this way.");
        }
        return (SentEmail) email;
    }

    private String nextId() {
        return "e" + idSequence.incrementAndGet();
    }
}
