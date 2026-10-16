package com.smartreply.ai.strategy;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;

import java.util.List;

/**
 * OOP concept - Abstraction + the Template Method pattern: this class fixes
 * the overall shape of every generated reply (greeting, then body, then
 * closing) and leaves the three pieces of wording to subclasses via
 * abstract methods. generate() itself is {@code final} so no subclass can
 * break that shared structure - only the wording changes per tone.
 * <p>
 * Shared helper methods below only ever reuse facts already present in the
 * original email / keyword analysis (sender name, detected dates, detected
 * action items) - nothing is invented, per the "simulated AI" requirement.
 */
public abstract class AbstractReplyStrategy implements ReplyStrategy {

    @Override
    public final String generate(Email originalEmail, KeywordAnalysis analysis) {
        String name = extractFirstName(originalEmail.getDisplayName());
        StringBuilder reply = new StringBuilder();
        reply.append(greeting(name)).append("\n\n");
        reply.append(bodyText(originalEmail, analysis));
        reply.append("\n\n").append(closing());
        return reply.toString();
    }

    protected abstract String greeting(String name);

    protected abstract String bodyText(Email originalEmail, KeywordAnalysis analysis);

    protected abstract String closing();

    protected String extractFirstName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "there";
        }
        String namePart = displayName.contains("@") ? displayName.split("@")[0] : displayName;
        String firstToken = namePart.split("[.\\s_]+")[0];
        if (firstToken.isBlank()) {
            return "there";
        }
        return Character.toUpperCase(firstToken.charAt(0)) + firstToken.substring(1).toLowerCase();
    }

    protected String deadlineSentence(KeywordAnalysis analysis, String prefix) {
        List<String> dates = analysis.getDates();
        if (dates.isEmpty()) {
            return "";
        }
        return prefix + dates.get(0) + ". ";
    }

    protected String actionItemsSentence(KeywordAnalysis analysis, String prefix) {
        List<String> items = analysis.getActionItems();
        if (items.isEmpty()) {
            return "";
        }
        String joined = items.stream()
                .map(item -> item.replaceAll("[.!?]+$", ""))
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
        return prefix + joined + ". ";
    }
}
