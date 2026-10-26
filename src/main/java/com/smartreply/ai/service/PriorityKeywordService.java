package com.smartreply.ai.service;

import com.smartreply.ai.model.Email;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Personal "priority keywords" for the signed-in user. When an email mentions
 * one of these words or phrases (in the subject, body or sender), it is flagged
 * and moved to the top of the list.
 * <p>
 * Matching is case-insensitive and on whole words, so the keyword "exam" matches
 * "Exam schedule" and "the exam." but not "example". A keyword can also be a short
 * phrase such as "purchase order".
 * <p>
 * {@code @SessionScope} for the same reason as {@link EmailService}: every signed-in
 * user must have their own list, never one shared by everybody. The dashboard also
 * keeps a copy in the browser so the list survives signing out or a server restart
 * (there is no database).
 */
@Service
@SessionScope
public class PriorityKeywordService {

    public static final int MAX_KEYWORDS = 20;
    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 40;

    /** Lower-cased keyword -> compiled whole-word pattern. Insertion order is kept for display. */
    private final Map<String, Pattern> keywords = new LinkedHashMap<>();

    public synchronized List<String> getKeywords() {
        return new ArrayList<>(keywords.keySet());
    }

    /**
     * Adds one keyword. Adding one that already exists is not an error, it simply changes nothing.
     *
     * @throws IllegalArgumentException if the keyword is empty, too short or long, or the list is full
     */
    public synchronized List<String> add(String raw) {
        String keyword = normalise(raw);
        if (keyword.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("A keyword needs at least " + MIN_LENGTH + " characters.");
        }
        if (keyword.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("A keyword can be at most " + MAX_LENGTH + " characters.");
        }
        if (!keywords.containsKey(keyword)) {
            if (keywords.size() >= MAX_KEYWORDS) {
                throw new IllegalArgumentException(
                        "You can save up to " + MAX_KEYWORDS + " keywords. Remove one to add another.");
            }
            keywords.put(keyword, compile(keyword));
        }
        return getKeywords();
    }

    public synchronized List<String> remove(String raw) {
        keywords.remove(normalise(raw));
        return getKeywords();
    }

    /** Replaces the whole list. Invalid or duplicate entries are skipped instead of failing the whole request. */
    public synchronized List<String> replaceAll(List<String> newKeywords) {
        keywords.clear();
        if (newKeywords != null) {
            for (String candidate : newKeywords) {
                String keyword = normalise(candidate);
                boolean valid = keyword.length() >= MIN_LENGTH && keyword.length() <= MAX_LENGTH;
                if (valid && keywords.size() < MAX_KEYWORDS && !keywords.containsKey(keyword)) {
                    keywords.put(keyword, compile(keyword));
                }
            }
        }
        return getKeywords();
    }

    /** The saved keywords that appear in this email, in the order the user saved them. */
    public synchronized List<String> matches(Email email) {
        if (keywords.isEmpty() || email == null) {
            return List.of();
        }
        String text = searchableText(email);
        List<String> found = new ArrayList<>();
        for (Map.Entry<String, Pattern> entry : keywords.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                found.add(entry.getKey());
            }
        }
        return found;
    }

    /**
     * Puts emails that match more keywords first. The sort is stable, so emails with the
     * same number of matches keep the order they came in (newest first).
     */
    public synchronized List<Email> prioritise(List<Email> emails) {
        List<Email> sorted = new ArrayList<>(emails);
        Map<String, Integer> score = new LinkedHashMap<>();
        for (Email email : sorted) {
            score.put(email.getId(), matches(email).size());
        }
        sorted.sort(Comparator.comparingInt((Email e) -> score.get(e.getId())).reversed());
        return sorted;
    }

    private static String searchableText(Email email) {
        return String.join("\n",
                nullToEmpty(email.getSubject()),
                nullToEmpty(email.getBody()),
                nullToEmpty(email.getFromName()),
                nullToEmpty(email.getFromAddress()));
    }

    private static Pattern compile(String keyword) {
        // Words are matched literally but may be separated by any spaces or line breaks, so the phrase
        // "purchase order" also matches "Purchase   Order" and text wrapped across two lines.
        String phrase = String.join("\\s+", java.util.Arrays.stream(keyword.split(" "))
                .map(Pattern::quote).toList());
        // Not preceded or followed by a letter or digit: whole-word matching that also works for phrases.
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + phrase + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private static String normalise(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
