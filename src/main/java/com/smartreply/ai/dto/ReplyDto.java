package com.smartreply.ai.dto;

import com.smartreply.ai.model.Reply;
import com.smartreply.ai.model.Tone;

import java.time.LocalDateTime;

public class ReplyDto {

    private final String id;
    private final String originalEmailId;
    private final Tone tone;
    private final String content;
    private final LocalDateTime generatedAt;
    private final boolean edited;
    private final int estimatedSecondsSaved;

    private ReplyDto(String id, String originalEmailId, Tone tone, String content,
                      LocalDateTime generatedAt, boolean edited, int estimatedSecondsSaved) {
        this.id = id;
        this.originalEmailId = originalEmailId;
        this.tone = tone;
        this.content = content;
        this.generatedAt = generatedAt;
        this.edited = edited;
        this.estimatedSecondsSaved = estimatedSecondsSaved;
    }

    public static ReplyDto from(Reply reply) {
        return new ReplyDto(reply.getId(), reply.getOriginalEmailId(), reply.getTone(), reply.getContent(),
                reply.getGeneratedAt(), reply.isEdited(), reply.getEstimatedSecondsSaved());
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

    public String getContent() {
        return content;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public int getEstimatedSecondsSaved() {
        return estimatedSecondsSaved;
    }
}
