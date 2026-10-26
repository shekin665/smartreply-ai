package com.smartreply.ai.service;

import com.smartreply.ai.i18n.Phrase;
import com.smartreply.ai.i18n.PhraseBook;
import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.Tone;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Writes replies in languages other than English from the phrase files. English replies keep using the
 * strategy classes. The sender's subject, action items and dates are copied from the email as they are
 * written; only the fixed wording around them is localised.
 */
@Component
public class LocalizedReplyComposer {

    public static final String DEFAULT_SENDER = "SmartReply AI (Demo User)";

    private enum Style { FORMAL, FRIENDLY, CONCISE, APOLOGETIC }

    private final PhraseBook phrases;

    public LocalizedReplyComposer(PhraseBook phrases) {
        this.phrases = phrases;
    }

    public String compose(Language language, Tone tone, Email email, KeywordAnalysis analysis, String senderName) {
        Style style = styleFor(tone);
        boolean detailed = tone == Tone.DETAILED;
        boolean warm = style == Style.FRIENDLY || style == Style.CONCISE;

        String name = firstName(email.getDisplayName());
        String greeting = name.isEmpty()
                ? phrases.text(language, warm ? Phrase.GREETING_NONAME_FRIENDLY : Phrase.GREETING_NONAME_FORMAL)
                : phrases.text(language, warm ? Phrase.GREETING_FRIENDLY : Phrase.GREETING_FORMAL, name);

        StringBuilder body = new StringBuilder();
        body.append(phrases.text(language, open(style), nullToEmpty(email.getSubject())));

        List<String> items = analysis.getActionItems();
        if (!items.isEmpty()) {
            String joined = String.join("; ", items.stream().map(i -> i.replaceAll("[.!?]+$", "")).toList());
            body.append(' ').append(phrases.text(language, Phrase.REPLY_ACTIONS, joined));
        }
        if (!analysis.getDates().isEmpty()) {
            body.append(' ').append(phrases.text(language, Phrase.REPLY_DEADLINE, analysis.getDates().get(0)));
        }
        if (detailed) {
            body.append(' ').append(phrases.text(language, Phrase.REPLY_DETAILED_EXTRA));
        }
        body.append(' ').append(phrases.text(language, end(style)));

        String sender = senderName == null || senderName.isBlank() ? DEFAULT_SENDER : senderName.trim();
        return greeting + "\n\n" + body + "\n\n" + phrases.text(language, closing(style)) + "\n" + sender;
    }

    private static Style styleFor(Tone tone) {
        return switch (tone) {
            case FRIENDLY -> Style.FRIENDLY;
            case CONCISE -> Style.CONCISE;
            case APOLOGETIC -> Style.APOLOGETIC;
            default -> Style.FORMAL; // PROFESSIONAL, FORMAL, DETAILED
        };
    }

    private static Phrase open(Style s) {
        return switch (s) {
            case FRIENDLY -> Phrase.REPLY_OPEN_FRIENDLY;
            case CONCISE -> Phrase.REPLY_OPEN_CONCISE;
            case APOLOGETIC -> Phrase.REPLY_OPEN_APOLOGETIC;
            default -> Phrase.REPLY_OPEN_FORMAL;
        };
    }

    private static Phrase end(Style s) {
        return switch (s) {
            case FRIENDLY -> Phrase.REPLY_END_FRIENDLY;
            case CONCISE -> Phrase.REPLY_END_CONCISE;
            case APOLOGETIC -> Phrase.REPLY_END_APOLOGETIC;
            default -> Phrase.REPLY_END_FORMAL;
        };
    }

    private static Phrase closing(Style s) {
        return switch (s) {
            case FRIENDLY -> Phrase.CLOSING_FRIENDLY;
            case CONCISE -> Phrase.CLOSING_CONCISE;
            case APOLOGETIC -> Phrase.CLOSING_APOLOGETIC;
            default -> Phrase.CLOSING_FORMAL;
        };
    }

    /** First word of the sender's display name or address, or "" when there is none. */
    private static String firstName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String namePart = displayName.contains("@") ? displayName.split("@")[0] : displayName;
        String[] tokens = namePart.split("[.\\s_]+");
        String first = tokens.length == 0 ? "" : tokens[0];
        if (first.isBlank()) {
            return "";
        }
        return Character.toUpperCase(first.charAt(0)) + first.substring(1).toLowerCase();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
