package com.smartreply.ai.model;

import java.util.List;

/**
 * Result of running KeywordService's rule-based "keyword intelligence"
 * engine over one email. Plain data holder (encapsulated, immutable after
 * construction) returned to the controller layer as-is.
 */
public class KeywordAnalysis {

    private final List<String> keywords;
    private final List<String> dates;
    private final List<String> actionItems;
    private final Priority priority;

    public KeywordAnalysis(List<String> keywords, List<String> dates,
                            List<String> actionItems, Priority priority) {
        this.keywords = keywords;
        this.dates = dates;
        this.actionItems = actionItems;
        this.priority = priority;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public List<String> getDates() {
        return dates;
    }

    public List<String> getActionItems() {
        return actionItems;
    }

    public Priority getPriority() {
        return priority;
    }
}
