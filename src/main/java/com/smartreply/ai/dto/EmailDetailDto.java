package com.smartreply.ai.dto;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;

import java.time.LocalDateTime;

/** Full email detail shown in the reader pane. */
public class EmailDetailDto {

    private final String id;
    private final String fromAddress;
    private final String fromName;
    private final String toAddress;
    private final String displayName;
    private final String directionLabel;
    private final String subject;
    private final String body;
    private final LocalDateTime date;
    private final Folder folder;
    private final boolean read;
    private final boolean starred;
    private final boolean isReceived;
    private final boolean fromRealGmail;

    private EmailDetailDto(String id, String fromAddress, String fromName, String toAddress, String displayName,
                            String directionLabel, String subject, String body, LocalDateTime date, Folder folder,
                            boolean read, boolean starred, boolean isReceived, boolean fromRealGmail) {
        this.id = id;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.toAddress = toAddress;
        this.displayName = displayName;
        this.directionLabel = directionLabel;
        this.subject = subject;
        this.body = body;
        this.date = date;
        this.folder = folder;
        this.read = read;
        this.starred = starred;
        this.isReceived = isReceived;
        this.fromRealGmail = fromRealGmail;
    }

    public static EmailDetailDto from(Email email) {
        return new EmailDetailDto(
                email.getId(), email.getFromAddress(), email.getFromName(), email.getToAddress(),
                email.getDisplayName(), email.getDirectionLabel(), email.getSubject(), email.getBody(),
                email.getDate(), email.getFolder(), email.isRead(), email.isStarred(), email.isReceivedEmail(),
                email.isFromRealGmail()
        );
    }

    public String getId() {
        return id;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToAddress() {
        return toAddress;
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

    public String getBody() {
        return body;
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
}
