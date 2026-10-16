package com.smartreply.ai.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class for every message in the mailbox.
 * <p>
 * OOP concept - Encapsulation: every field is private and reachable only
 * through the getters/setters below, so callers cannot put an Email into an
 * inconsistent state (e.g. a null subject or a blank sender).
 * <p>
 * OOP concept - Abstraction: Email models what every message has in common
 * (sender/recipient, subject, body, folder, flags) and leaves how the
 * "display name" of the other party is computed to subclasses.
 */
public abstract class Email {

    private final String id;
    private String fromAddress;
    private String fromName;
    private String toAddress;
    private String subject;
    private String body;
    private LocalDateTime date;
    private Folder folder;
    private boolean read;
    private boolean starred;
    /** True only for messages imported from a real Gmail account via IMAP sync - false for all seeded/demo and user-composed emails. */
    private boolean fromRealGmail;

    protected Email(String id, String fromAddress, String fromName, String toAddress,
                     String subject, String body, LocalDateTime date, Folder folder) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.toAddress = toAddress;
        this.subject = subject;
        this.body = body;
        this.date = date;
        this.folder = folder;
        this.read = false;
        this.starred = false;
    }

    /**
     * OOP concept - Polymorphism: each subclass decides whose name should be
     * shown in the message list (the sender for a received email, the
     * recipient for one the user authored).
     */
    public abstract String getDisplayName();

    /** OOP concept - Polymorphism: a short label describing the email's origin. */
    public abstract String getDirectionLabel();

    /**
     * Folder this email belongs in when "restored" out of SPAM/TRASH/ARCHIVE.
     * Overridden per subclass instead of branching on instanceof in EmailService.
     */
    public abstract Folder homeFolder();

    /** True for ReceivedEmail, false for SentEmail - lets callers avoid instanceof checks. */
    public abstract boolean isReceivedEmail();

    public String getId() {
        return id;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToAddress() {
        return toAddress;
    }

    public void setToAddress(String toAddress) {
        this.toAddress = toAddress;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public boolean isFromRealGmail() {
        return fromRealGmail;
    }

    public void setFromRealGmail(boolean fromRealGmail) {
        this.fromRealGmail = fromRealGmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email)) return false;
        Email other = (Email) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
