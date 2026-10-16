package com.smartreply.ai.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of POST /api/emails/draft and POST /api/emails/send - compose/edit a draft or send an email. */
public class ComposeRequest {

    /** Null/blank when composing a brand-new draft; set when editing an existing one. */
    private String id;

    @NotBlank(message = "is required")
    private String to;

    @NotBlank(message = "is required")
    private String subject;

    private String body;

    /** Set when this email is a reply generated from another email (for traceability only). */
    private String repliedToEmailId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
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

    public String getRepliedToEmailId() {
        return repliedToEmailId;
    }

    public void setRepliedToEmailId(String repliedToEmailId) {
        this.repliedToEmailId = repliedToEmailId;
    }
}
