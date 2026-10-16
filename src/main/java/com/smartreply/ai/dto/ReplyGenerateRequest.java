package com.smartreply.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.smartreply.ai.model.Tone;

/** Body of POST /api/replies/generate. */
public class ReplyGenerateRequest {

    @NotBlank(message = "is required")
    private String emailId;

    @NotNull(message = "is required")
    private Tone tone;

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public Tone getTone() {
        return tone;
    }

    public void setTone(Tone tone) {
        this.tone = tone;
    }
}
