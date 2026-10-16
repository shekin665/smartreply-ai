package com.smartreply.ai.model;

/**
 * Priority level assigned by KeywordService based on urgency words and
 * how close any detected deadline is.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}
