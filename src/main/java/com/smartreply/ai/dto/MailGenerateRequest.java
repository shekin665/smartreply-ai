package com.smartreply.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.MailKind;
import com.smartreply.ai.model.Tone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** Body of POST /api/mail/generate: the event details the mail is written from. */
public class MailGenerateRequest {

    @NotNull(message = "is required")
    private MailKind kind;

    @NotBlank(message = "is required")
    @Size(max = 120, message = "must be at most 120 characters")
    private String eventTitle;

    @NotNull(message = "is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    @Size(max = 120, message = "must be at most 120 characters")
    private String venue;

    @Size(max = 600, message = "must be at most 600 characters")
    private String keyPoints;

    @Size(max = 60, message = "must be at most 60 characters")
    private String recipientName;

    /** The style of the mail. Defaults to FORMAL. */
    private Tone tone = Tone.FORMAL;

    private Language language = Language.ENGLISH;

    public MailKind getKind() { return kind; }
    public void setKind(MailKind kind) { this.kind = kind; }
    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getKeyPoints() { return keyPoints; }
    public void setKeyPoints(String keyPoints) { this.keyPoints = keyPoints; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public Tone getTone() { return tone; }
    public void setTone(Tone tone) { this.tone = tone == null ? Tone.FORMAL : tone; }
    public Language getLanguage() { return language; }
    public void setLanguage(Language language) { this.language = language == null ? Language.ENGLISH : language; }
}
