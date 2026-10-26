package com.smartreply.ai.service;

import com.smartreply.ai.dto.MailGenerateRequest;
import com.smartreply.ai.i18n.Phrase;
import com.smartreply.ai.i18n.PhraseBook;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.MailKind;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes a complete mail (subject and body) from event details: the type of mail, event title, date, time,
 * venue and a few key points. The wording around those details comes from the phrase files, so the same
 * details can be written in any supported language and in several styles. Nothing the user typed is
 * translated or changed, except for tidying whitespace.
 */
@Service
public class MailGeneratorService {

    public static final int MAX_KEY_POINTS = 8;
    public static final int MAX_POINT_LENGTH = 140;

    public record GeneratedMail(String subject, String body) { }

    private enum Style { FORMAL, FRIENDLY, CONCISE, DETAILED }

    private final PhraseBook phrases;

    public MailGeneratorService(PhraseBook phrases) {
        this.phrases = phrases;
    }

    public GeneratedMail generate(MailGenerateRequest request, String senderName) {
        if (request == null || request.getKind() == null) {
            throw new IllegalArgumentException("Choose the type of mail.");
        }
        String title = oneLine(request.getEventTitle());
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Enter the event title.");
        }
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Choose the event date.");
        }

        Language language = request.getLanguage() == null ? Language.ENGLISH : request.getLanguage();
        Style style = styleFor(request.getTone());
        boolean warm = style == Style.FRIENDLY || style == Style.CONCISE;
        boolean friendlyWording = warm;

        String fullDate = formatDate(language, request.getDate(), FormatStyle.FULL);
        String mediumDate = formatDate(language, request.getDate(), FormatStyle.MEDIUM);
        String dateText = (style == Style.FORMAL || style == Style.DETAILED) ? fullDate : mediumDate;
        String time = request.getTime() == null ? "" : formatTime(language, request);
        String venue = oneLine(request.getVenue());
        List<String> points = splitPoints(request.getKeyPoints());

        String subject = phrases.text(language, subjectPhrase(request.getKind()), title) + " – " + mediumDate;

        String recipient = oneLine(request.getRecipientName());
        String greeting = recipient.isEmpty()
                ? phrases.text(language, warm ? Phrase.GREETING_NONAME_FRIENDLY : Phrase.GREETING_NONAME_FORMAL)
                : phrases.text(language, warm ? Phrase.GREETING_FRIENDLY : Phrase.GREETING_FORMAL, recipient);
        String lead = phrases.text(language, leadPhrase(request.getKind(), friendlyWording));
        String ask = phrases.text(language, friendlyWording ? Phrase.ASK_CONFIRM_FRIENDLY : Phrase.ASK_CONFIRM_FORMAL);

        StringBuilder body = new StringBuilder(greeting).append("\n\n").append(lead).append("\n");
        if (style == Style.CONCISE) {
            body.append(title).append("\n");
            List<String> details = new ArrayList<>();
            details.add(dateText);
            if (!time.isEmpty()) details.add(time);
            if (!venue.isEmpty()) details.add(venue);
            body.append(String.join(" | ", details)).append("\n");
            if (!points.isEmpty()) {
                body.append(String.join("; ", points)).append("\n");
            }
            body.append("\n");
        } else {
            body.append(label(language, Phrase.LABEL_EVENT, title)).append("\n");
            body.append(label(language, Phrase.LABEL_DATE, dateText)).append("\n");
            if (!time.isEmpty()) body.append(label(language, Phrase.LABEL_TIME, time)).append("\n");
            if (!venue.isEmpty()) body.append(label(language, Phrase.LABEL_VENUE, venue)).append("\n");
            body.append("\n");
            if (!points.isEmpty()) {
                body.append(phrases.text(language, Phrase.LABEL_POINTS)).append(":\n");
                for (String point : points) {
                    body.append("• ").append(point).append("\n");
                }
                body.append("\n");
            }
        }
        body.append(ask).append("\n\n");
        if (style == Style.DETAILED) {
            body.append(phrases.text(language, Phrase.DETAILED_EXTRA)).append("\n\n");
        }
        String sender = senderName == null || senderName.isBlank()
                ? LocalizedReplyComposer.DEFAULT_SENDER : senderName.trim();
        body.append(phrases.text(language, closingPhrase(style))).append("\n").append(sender);

        return new GeneratedMail(subject, body.toString());
    }

    /** Splits typed key points on new lines or semicolons, drops bullets and blanks, and caps count and length. */
    static List<String> splitPoints(String raw) {
        List<String> points = new ArrayList<>();
        if (raw == null) {
            return points;
        }
        for (String part : raw.split("[\\r\\n;]+")) {
            String point = oneLine(part.replaceFirst("^\\s*[-•*]+\\s*", ""));
            if (point.isEmpty()) continue;
            if (point.length() > MAX_POINT_LENGTH) point = point.substring(0, MAX_POINT_LENGTH).trim();
            points.add(point);
            if (points.size() == MAX_KEY_POINTS) break;
        }
        return points;
    }

    private String label(Language language, Phrase label, String value) {
        return phrases.text(language, label) + ": " + value;
    }

    private static Style styleFor(Tone tone) {
        if (tone == null) return Style.FORMAL;
        return switch (tone) {
            case FRIENDLY -> Style.FRIENDLY;
            case CONCISE -> Style.CONCISE;
            case DETAILED -> Style.DETAILED;
            default -> Style.FORMAL; // FORMAL, PROFESSIONAL, APOLOGETIC
        };
    }

    private static Phrase subjectPhrase(MailKind kind) {
        return switch (kind) {
            case INVITATION -> Phrase.SUBJECT_INVITATION;
            case REMINDER -> Phrase.SUBJECT_REMINDER;
            case MEETING_REQUEST -> Phrase.SUBJECT_MEETING_REQUEST;
        };
    }

    private static Phrase leadPhrase(MailKind kind, boolean friendly) {
        return switch (kind) {
            case INVITATION -> friendly ? Phrase.LEAD_INVITATION_FRIENDLY : Phrase.LEAD_INVITATION_FORMAL;
            case REMINDER -> friendly ? Phrase.LEAD_REMINDER_FRIENDLY : Phrase.LEAD_REMINDER_FORMAL;
            case MEETING_REQUEST -> friendly ? Phrase.LEAD_MEETING_REQUEST_FRIENDLY : Phrase.LEAD_MEETING_REQUEST_FORMAL;
        };
    }

    private static Phrase closingPhrase(Style style) {
        return switch (style) {
            case FRIENDLY -> Phrase.CLOSING_FRIENDLY;
            case CONCISE -> Phrase.CLOSING_CONCISE;
            default -> Phrase.CLOSING_FORMAL;
        };
    }

    private static String formatDate(Language language, java.time.LocalDate date, FormatStyle style) {
        if (language == Language.ENGLISH) {
            String pattern = style == FormatStyle.FULL ? "EEEE, d MMMM yyyy" : "d MMM yyyy";
            return DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH).format(date);
        }
        return normaliseSpaces(DateTimeFormatter.ofLocalizedDate(style).withLocale(language.getLocale()).format(date));
    }

    private static String formatTime(Language language, MailGenerateRequest request) {
        if (language == Language.ENGLISH) {
            return DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).format(request.getTime());
        }
        return normaliseSpaces(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                .withLocale(language.getLocale()).format(request.getTime()));
    }

    /** Newer Java versions use a narrow no-break space before AM/PM; mail clients show it as a box. */
    private static String normaliseSpaces(String s) {
        return s.replace(' ', ' ').replace(' ', ' ');
    }

    private static String oneLine(String s) {
        return s == null ? "" : s.replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("\\s+", " ");
    }
}
