package com.smartreply.ai.model;

/** The kinds of mail the mail generator can write from an event's details. */
public enum MailKind {
    INVITATION("Invitation"),
    REMINDER("Reminder"),
    MEETING_REQUEST("Meeting request");

    private final String label;

    MailKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
