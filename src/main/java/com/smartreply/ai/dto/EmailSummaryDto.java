package com.smartreply.ai.dto;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;

import java.time.LocalDateTime;
import java.util.List;

/** Lightweight shape used for email list views (inbox/sent/drafts/spam rows). */
public class EmailSummaryDto {

    private final String id;
    private final String displayName;
    private final String directionLabel;
    private final String subject;
    private final String snippet;
    private final LocalDateTime date;
    private final Folder folder;
    private final boolean read;
    private final boolean starred;
    private final boolean isReceived;
    private final boolean fromRealGmail;
    private final List<String> matchedKeywords;

    private EmailSummaryDto(String id, String displayName, String directionLabel, String subject, String snippet,
                             LocalDateTime date, Folder folder, boolean read, boolean starred, boolean isReceived,
                             boolean fromRealGmail, List<String> matchedKeywords) {
        this.id = id;
        this.displayName = displayName;
        this.directionLabel = directionLabel;
        this.subject = subject;
        this.snippet = snippet;
        this.date = date;
        this.folder = folder;
        this.read = read;
        this.starred = starred;
        this.isReceived = isReceived;
        this.fromRealGmail = fromRealGmail;
        this.matchedKeywords = matchedKeywords;
    }

    public static EmailSummaryDto from(Email email) {
        return from(email, List.of());
    }

    /** Same as {@link #from(Email)}, plus the user's priority keywords found in this email. */
    public static EmailSummaryDto from(Email email, List<String> matchedKeywords) {
        String body = email.getBody() == null ? "" : email.getBody().replaceAll("\\s+", " ").trim();
        String snippet = body.length() > 110 ? body.substring(0, 110) + "…" : body;
        return new EmailSummaryDto(
                email.getId(),
                email.getDisplayName(),
                email.getDirectionLabel(),
                email.getSubject(),
                snippet,
                email.getDate(),
                email.getFolder(),
                email.isRead(),
                email.isStarred(),
                email.isReceivedEmail(),
                email.isFromRealGmail(),
                matchedKeywords
        );
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDirectionLabel() {
        return directionLabel;
    }

    public String getSubject() {
        return subject;
    }

    public String getSnippet() {
        return snippet;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public Folder getFolder() {
        return folder;
    }

    public boolean isRead() {
        return read;
    }

    public boolean isStarred() {
        return starred;
    }

    public boolean isReceived() {
        return isReceived;
    }

    public boolean isFromRealGmail() {
        return fromRealGmail;
    }

    public List<String> getMatchedKeywords() {
        return matchedKeywords;
    }
}
