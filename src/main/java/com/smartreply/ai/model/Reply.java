package com.smartreply.ai.model;

import java.time.LocalDateTime;

/**
 * A reply generated (or hand-edited) by the simulated AI engine for one
 * original email. Kept separate from Email because a reply is a draft of
 * text tied to a tone and a source message, not a mailbox item in its own
 * right until the user explicitly saves/sends it.
 */
public class Reply {

    private final String id;
    private final String originalEmailId;
    private Tone tone;
    private String content;
    private final LocalDateTime generatedAt;
    private boolean edited;
    private int estimatedSecondsSaved;
    private Language language = Language.ENGLISH;

    public Reply(String id, String originalEmailId, Tone tone, String content,
                 LocalDateTime generatedAt, int estimatedSecondsSaved) {
        this.id = id;
        this.originalEmailId = originalEmailId;
        this.tone = tone;
        this.content = content;
        this.generatedAt = generatedAt;
        this.edited = false;
        this.estimatedSecondsSaved = estimatedSecondsSaved;
    }

    public Reply(String id, String originalEmailId, Tone tone, String content,
                 LocalDateTime generatedAt, int estimatedSecondsSaved, Language language) {
        this(id, originalEmailId, tone, content, generatedAt, estimatedSecondsSaved);
        this.language = language == null ? Language.ENGLISH : language;
    }

    public Language getLanguage() {
        return language;
    }

    public String getId() {
        return id;
    }

    public String getOriginalEmailId() {
        return originalEmailId;
    }

    public Tone getTone() {
        return tone;
    }

    public void setTone(Tone tone) {
        this.tone = tone;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public int getEstimatedSecondsSaved() {
        return estimatedSecondsSaved;
    }

    public void setEstimatedSecondsSaved(int estimatedSecondsSaved) {
        this.estimatedSecondsSaved = estimatedSecondsSaved;
    }
}
