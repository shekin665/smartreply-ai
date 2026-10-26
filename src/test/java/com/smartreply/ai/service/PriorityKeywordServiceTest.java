package com.smartreply.ai.service;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;
import com.smartreply.ai.model.ReceivedEmail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriorityKeywordServiceTest {

    private PriorityKeywordService service;

    @BeforeEach
    void setUp() {
        service = new PriorityKeywordService();
    }

    private static Email mail(String id, String from, String subject, String body, int minutesAgo) {
        return new ReceivedEmail(id, from + "@example.com", from, "me@example.com", subject, body,
                LocalDateTime.of(2026, 10, 26, 12, 0).minusMinutes(minutesAgo), Folder.INBOX);
    }

    @Test
    void matchingIsCaseInsensitiveAndFindsKeywordsInSubjectBodyAndSender() {
        service.add("Exam");
        service.add("meena");
        service.add("invoice");

        assertEquals(List.of("exam"), service.matches(mail("1", "Registrar", "EXAM schedule", "See attached.", 0)));
        assertEquals(List.of("invoice"), service.matches(mail("2", "Accounts", "Hello", "Your invoice is ready.", 0)));
        assertEquals(List.of("meena"), service.matches(mail("3", "Prof. Meena", "Hello", "Hi there.", 0)));
    }

    @Test
    void matchesWholeWordsOnly() {
        service.add("exam");

        assertTrue(service.matches(mail("1", "A", "Example of a good email", "Nothing here.", 0)).isEmpty());
        assertTrue(service.matches(mail("2", "A", "Hi", "Examination dates", 0)).isEmpty());
        assertFalse(service.matches(mail("3", "A", "Hi", "Is the exam tomorrow?", 0)).isEmpty());
    }

    @Test
    void phrasesMatchAcrossExtraSpacesAndLineBreaksAndSpecialCharactersAreLiteral() {
        service.add("purchase order");
        service.add("c++");

        assertEquals(List.of("purchase order"), service.matches(mail("1", "A", "New Purchase   Order", "Please confirm.", 0)));
        assertEquals(List.of("purchase order"), service.matches(mail("2", "A", "Hi", "Your purchase\norder is attached", 0)));
        assertTrue(service.matches(mail("3", "A", "Hi", "Your purchases and orders", 0)).isEmpty());
        assertEquals(List.of("c++"), service.matches(mail("4", "A", "Hi", "Learning c++ this week", 0)));
    }

    @Test
    void prioritiseMovesMatchingMailFirstAndKeepsOtherwiseTheSameOrder() {
        service.add("exam");
        service.add("urgent");
        Email newest = mail("newest", "A", "Lunch", "Pizza?", 1);
        Email oneMatch = mail("one", "B", "Exam hall", "Details", 30);
        Email twoMatches = mail("two", "C", "Urgent exam change", "Details", 60);
        Email older = mail("older", "D", "Newsletter", "News", 90);

        List<Email> result = service.prioritise(List.of(newest, oneMatch, twoMatches, older));

        assertEquals(List.of("two", "one", "newest", "older"), result.stream().map(Email::getId).toList());
    }

    @Test
    void prioritiseWithNoKeywordsLeavesTheOrderAlone() {
        Email a = mail("a", "A", "One", "x", 1);
        Email b = mail("b", "B", "Two", "y", 2);

        assertEquals(List.of("a", "b"), service.prioritise(List.of(a, b)).stream().map(Email::getId).toList());
    }

    @Test
    void addNormalisesIgnoresDuplicatesAndKeepsOrder() {
        service.add("  Exam  ");
        service.add("EXAM");
        service.add("Invoice");

        assertEquals(List.of("exam", "invoice"), service.getKeywords());
    }

    @Test
    void addRejectsTooShortEmptyAndTooLongKeywords() {
        assertThrows(IllegalArgumentException.class, () -> service.add(null));
        assertThrows(IllegalArgumentException.class, () -> service.add("   "));
        assertThrows(IllegalArgumentException.class, () -> service.add("a"));
        assertThrows(IllegalArgumentException.class, () -> service.add("x".repeat(PriorityKeywordService.MAX_LENGTH + 1)));
    }

    @Test
    void addRejectsKeywordsBeyondTheLimit() {
        for (int i = 0; i < PriorityKeywordService.MAX_KEYWORDS; i++) {
            service.add("keyword" + i);
        }
        assertThrows(IllegalArgumentException.class, () -> service.add("one-too-many"));
        // an existing keyword can still be "added" again without error
        assertDoesNotThrow(() -> service.add("keyword0"));
    }

    @Test
    void removeIsCaseInsensitiveAndForgivesUnknownKeywords() {
        service.add("exam");
        assertEquals(List.of(), service.remove("EXAM"));
        assertEquals(List.of(), service.remove("never-added"));
    }

    @Test
    void replaceAllSkipsInvalidAndDuplicateEntries() {
        List<String> result = service.replaceAll(List.of("exam", "EXAM", "", "x", "invoice"));
        assertEquals(List.of("exam", "invoice"), result);
        assertEquals(List.of(), service.replaceAll(null));
    }
}
