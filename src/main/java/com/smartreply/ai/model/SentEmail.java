package com.smartreply.ai.model;

import java.time.LocalDateTime;

/**
 * An email authored by the demo user - either already sent (folder SENT) or
 * still being written (folder DRAFTS). The same class covers both cases
 * because in both the user is the author; only the {@code sent} flag and
 * folder differ.
 * <p>
 * OOP concept - Inheritance + Polymorphism: overrides the same two methods
 * as ReceivedEmail, but resolves them from the recipient's point of view.
 */
public class SentEmail extends Email {

    private boolean sent;
    private boolean generatedByAi;
    private String repliedToEmailId;

    public SentEmail(String id, String fromAddress, String fromName, String toAddress,
                      String subject, String body, LocalDateTime date, Folder folder,
                      boolean sent) {
        super(id, fromAddress, fromName, toAddress, subject, body, date, folder);
        this.sent = sent;
    }

    @Override
    public String getDisplayName() {
        return getToAddress();
    }

    @Override
    public String getDirectionLabel() {
        return "to";
    }

    @Override
    public Folder homeFolder() {
        return sent ? Folder.SENT : Folder.DRAFTS;
    }

    @Override
    public boolean isReceivedEmail() {
        return false;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public boolean isGeneratedByAi() {
        return generatedByAi;
    }

    public void setGeneratedByAi(boolean generatedByAi) {
        this.generatedByAi = generatedByAi;
    }

    public String getRepliedToEmailId() {
        return repliedToEmailId;
    }

    public void setRepliedToEmailId(String repliedToEmailId) {
        this.repliedToEmailId = repliedToEmailId;
    }
}
