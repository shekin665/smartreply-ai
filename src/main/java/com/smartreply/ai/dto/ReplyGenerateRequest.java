package com.smartreply.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.Tone;
import jakarta.validation.constraints.Size;

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

    private Language language = Language.ENGLISH;

    @Size(max = 300, message = "must be at most 300 characters")
    private String extraNote;

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language == null ? Language.ENGLISH : language;
    }

    public String getExtraNote() {
        return extraNote;
    }

    public void setExtraNote(String extraNote) {
        this.extraNote = extraNote;
    }
}
