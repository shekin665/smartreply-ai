package com.smartreply.ai.dto;

import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.Tone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/mail/assist: the user's own text, plus what to do with it. */
public class TextAssistRequest {

    @NotBlank(message = "is required")
    @Size(max = 4000, message = "must be at most 4000 characters")
    private String text;

    /** Translate into this language. Null keeps the text's own language. */
    private Language language;

    /** Rewrite in this style. Null keeps the user's own style and only fixes mistakes. */
    private Tone style;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language; }
    public Tone getStyle() { return style; }
    public void setStyle(Tone style) { this.style = style; }
}
