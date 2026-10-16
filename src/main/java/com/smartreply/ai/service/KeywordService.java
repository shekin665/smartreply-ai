package com.smartreply.ai.service;

import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Priority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Keyword Intelligence" - a small, fully rule-based Java engine (no ML,
 * no external API) that looks at an email's subject/body and pulls out:
 * <ul>
 *   <li>frequent non-trivial words (keywords)</li>
 *   <li>date-looking phrases (dates)</li>
 *   <li>sentences that read like a request/instruction (action items)</li>
 *   <li>an overall {@link Priority} derived from urgency words and dates</li>
 * </ul>
 * Every method only reports what is literally present in the text - it
 * never guesses a date or action item that isn't written in the email.
 */
@Service
public class KeywordService {

    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]{3,}");

    private static final List<String> STOPWORDS = List.of(
            "the", "and", "for", "are", "but", "not", "you", "your", "all", "can", "has", "had",
            "was", "were", "with", "this", "that", "from", "have", "will", "would", "could",
            "should", "about", "into", "than", "then", "there", "their", "them", "our", "ours",
            "please", "thanks", "thank", "regards", "hello", "hi", "dear", "best", "sincerely",
            "ill", "its", "hey", "just", "also", "like", "been", "being", "they", "what", "when",
            "who", "how", "why", "yes", "out", "get", "got", "let", "know", "need", "needed"
    );

    private static final List<String> URGENCY_WORDS = List.of(
            "urgent", "asap", "immediately", "critical", "emergency", "deadline",
            "important", "priority", "overdue", "escalate", "final notice"
    );

    private static final List<String> ACTION_CUES = List.of(
            "please", "kindly", "could you", "can you", "would you", "need you to",
            "must", "required", "action required", "respond by", "reply by", "submit",
            "complete", "review", "attend", "confirm", "rsvp", "schedule", "approve",
            "sign", "upload", "register", "pay", "renew"
    );

    private static final String MONTHS =
            "(January|February|March|April|May|June|July|August|September|October|November|December" +
            "|Jan|Feb|Mar|Apr|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)";

    private static final List<Pattern> DATE_PATTERNS = List.of(
            Pattern.compile(MONTHS + "\\s+\\d{1,2}(st|nd|rd|th)?(,?\\s*\\d{4})?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\d{1,2}(st|nd|rd|th)?\\s+" + MONTHS + "(,?\\s*\\d{4})?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{1,2}[/-]\\d{1,2}([/-]\\d{2,4})?\\b"),
            Pattern.compile("\\b(today|tomorrow|tonight|end of day|EOD|this week|next week|" +
                    "this monday|this tuesday|this wednesday|this thursday|this friday|this weekend|" +
                    "next monday|next tuesday|next wednesday|next thursday|next friday|" +
                    "by monday|by tuesday|by wednesday|by thursday|by friday)\\b", Pattern.CASE_INSENSITIVE)
    );

    public KeywordAnalysis analyze(String subject, String body) {
        String safeSubject = subject == null ? "" : subject;
        String safeBody = body == null ? "" : body;
        String fullText = safeSubject + " " + safeBody;

        List<String> keywords = extractKeywords(fullText);
        List<String> dates = extractDates(fullText);
        List<String> actionItems = extractActionItems(safeBody);
        Priority priority = determinePriority(fullText, dates, actionItems);

        return new KeywordAnalysis(keywords, dates, actionItems, priority);
    }

    private List<String> extractKeywords(String text) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        Matcher matcher = WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase();
            if (STOPWORDS.contains(word)) {
                continue;
            }
            frequency.merge(word, 1, Integer::sum);
        }
        return frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> extractDates(String text) {
        LinkedHashSet<String> dates = new LinkedHashSet<>();
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                dates.add(matcher.group().trim());
            }
        }
        return new ArrayList<>(dates);
    }

    private List<String> extractActionItems(String body) {
        List<String> actionItems = new ArrayList<>();
        String[] sentences = body.split("(?<=[.!?\\n])\\s+");
        for (String rawSentence : sentences) {
            String sentence = rawSentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }
            String lower = sentence.toLowerCase();
            boolean looksLikeAction = ACTION_CUES.stream().anyMatch(lower::contains) || sentence.endsWith("?");
            if (looksLikeAction) {
                actionItems.add(truncate(sentence, 140));
            }
            if (actionItems.size() >= 5) {
                break;
            }
        }
        return actionItems;
    }

    private Priority determinePriority(String text, List<String> dates, List<String> actionItems) {
        String lower = text.toLowerCase();
        long urgencyHits = URGENCY_WORDS.stream().filter(lower::contains).count();
        boolean hasImminentDate = dates.stream().anyMatch(d -> {
            String dl = d.toLowerCase();
            return dl.contains("today") || dl.contains("tomorrow") || dl.contains("eod")
                    || dl.contains("end of day") || dl.contains("asap");
        });

        if (urgencyHits >= 2 || hasImminentDate) {
            return Priority.URGENT;
        }
        if (urgencyHits == 1 || (!dates.isEmpty() && !actionItems.isEmpty())) {
            return Priority.HIGH;
        }
        if (!dates.isEmpty() || !actionItems.isEmpty()) {
            return Priority.MEDIUM;
        }
        return Priority.LOW;
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1).trim() + "…";
    }
}
