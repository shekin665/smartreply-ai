package com.smartreply.ai.i18n;

/**
 * Every fixed piece of wording the mail and reply generators use. Each constant has a line with the same
 * name in every {@code i18n/phrases_<code>.properties} file. {@code {0}} marks where a value (a name, a
 * subject, a date) is inserted.
 */
public enum Phrase {
    // Greetings and closings
    GREETING_FORMAL,
    GREETING_FRIENDLY,
    GREETING_NONAME_FORMAL,
    GREETING_NONAME_FRIENDLY,
    CLOSING_FORMAL,
    CLOSING_FRIENDLY,
    CLOSING_CONCISE,
    CLOSING_APOLOGETIC,

    // Labels for the event details
    LABEL_EVENT,
    LABEL_DATE,
    LABEL_TIME,
    LABEL_VENUE,
    LABEL_POINTS,

    // Mail generator
    SUBJECT_INVITATION,
    SUBJECT_REMINDER,
    SUBJECT_MEETING_REQUEST,
    LEAD_INVITATION_FORMAL,
    LEAD_INVITATION_FRIENDLY,
    LEAD_REMINDER_FORMAL,
    LEAD_REMINDER_FRIENDLY,
    LEAD_MEETING_REQUEST_FORMAL,
    LEAD_MEETING_REQUEST_FRIENDLY,
    ASK_CONFIRM_FORMAL,
    ASK_CONFIRM_FRIENDLY,
    DETAILED_EXTRA,

    // Replies
    REPLY_OPEN_FORMAL,
    REPLY_OPEN_FRIENDLY,
    REPLY_OPEN_CONCISE,
    REPLY_OPEN_APOLOGETIC,
    REPLY_ACTIONS,
    REPLY_DEADLINE,
    REPLY_END_FORMAL,
    REPLY_END_FRIENDLY,
    REPLY_END_CONCISE,
    REPLY_END_APOLOGETIC,
    REPLY_DETAILED_EXTRA
}
