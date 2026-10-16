package com.smartreply.ai.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of PUT /api/replies/{id} - saving a user's edits to generated reply text. */
public class ReplyUpdateRequest {

    @NotBlank(message = "must not be empty")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
