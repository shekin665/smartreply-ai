package com.smartreply.ai.model;

/**
 * The folder an email currently lives in. An email moves between folders
 * (e.g. INBOX -> ARCHIVE, INBOX -> SPAM -> INBOX) via EmailService methods
 * rather than by being copied, so there is always exactly one owning folder.
 */
public enum Folder {
    INBOX,
    SENT,
    DRAFTS,
    SPAM,
    ARCHIVE,
    TRASH
}
