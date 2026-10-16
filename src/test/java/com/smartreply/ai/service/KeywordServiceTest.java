package com.smartreply.ai.service;

import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Priority;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeywordServiceTest {

    private final KeywordService keywordService = new KeywordService();

    @Test
    void detectsUrgentPriorityAndDeadlineAndActionItem() {
        String subject = "Urgent: Submit report";
        String body = "Please submit the final report by tomorrow. This is urgent, do not delay.";

        KeywordAnalysis analysis = keywordService.analyze(subject, body);

        assertEquals(Priority.URGENT, analysis.getPriority());
        assertTrue(analysis.getDates().stream().anyMatch(d -> d.toLowerCase().contains("tomorrow")));
        assertTrue(analysis.getActionItems().stream().anyMatch(a -> a.toLowerCase().contains("submit")));
    }

    @Test
    void plainEmailWithNoSignalsIsLowPriority() {
        KeywordAnalysis analysis = keywordService.analyze("Hello", "Just saying hi, hope you are doing well.");

        assertEquals(Priority.LOW, analysis.getPriority());
        assertTrue(analysis.getDates().isEmpty());
        assertTrue(analysis.getActionItems().isEmpty());
    }

    @Test
    void keywordExtractionExcludesStopwords() {
        KeywordAnalysis analysis = keywordService.analyze("Meeting", "The meeting and the agenda and the schedule.");

        assertFalse(analysis.getKeywords().contains("the"));
        assertFalse(analysis.getKeywords().contains("and"));
        assertTrue(analysis.getKeywords().contains("meeting"));
    }

    @Test
    void doesNotInventDatesWhenNoneArePresent() {
        KeywordAnalysis analysis = keywordService.analyze("Catching up", "It was great seeing you at the conference.");

        assertTrue(analysis.getDates().isEmpty());
    }
}
