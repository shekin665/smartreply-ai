package com.smartreply.ai.exception;

/** Thrown when a lookup by reply id finds nothing in ReplyService. */
public class ReplyNotFoundException extends RuntimeException {

    public ReplyNotFoundException(String replyId) {
        super("No generated reply found with id: " + replyId);
    }
}
