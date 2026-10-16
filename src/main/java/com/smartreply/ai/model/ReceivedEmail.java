package com.smartreply.ai.model;

import java.time.LocalDateTime;

/**
 * An email somebody else sent to the demo user. Lives in INBOX, SPAM,
 * ARCHIVE or TRASH.
 * <p>
 * OOP concept - Inheritance: reuses every field/behaviour of Email and only
 * adds what is specific to an incoming message.
 * <p>
 * OOP concept - Polymorphism / method overriding: getDisplayName() and
 * getDirectionLabel() are implemented differently from SentEmail, so code
 * that only knows about Email still shows the right name at runtime.
 */
public class ReceivedEmail extends Email {

    public ReceivedEmail(String id, String fromAddress, String fromName, String toAddress,
                          String subject, String body, LocalDateTime date, Folder folder) {
        super(id, fromAddress, fromName, toAddress, subject, body, date, folder);
    }

    @Override
    public String getDisplayName() {
        return getFromName() != null ? getFromName() : getFromAddress();
    }

    @Override
    public String getDirectionLabel() {
        return "from";
    }

    @Override
    public Folder homeFolder() {
        return Folder.INBOX;
    }

    @Override
    public boolean isReceivedEmail() {
        return true;
    }
}
