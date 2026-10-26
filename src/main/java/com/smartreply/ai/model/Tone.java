package com.smartreply.ai.model;

/**
 * The reply tones supported by the simulated AI engine.
 * Each value corresponds to exactly one ReplyStrategy implementation.
 */
public enum Tone {
    PROFESSIONAL("Professional"),
    FRIENDLY("Friendly"),
    FORMAL("Formal"),
    CONCISE("Concise"),
    APOLOGETIC("Apologetic"),
    DETAILED("Detailed");

    private final String label;

    Tone(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
