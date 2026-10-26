package com.smartreply.ai.i18n;

import com.smartreply.ai.model.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhraseBookTest {

    private final PhraseBook book = new PhraseBook();

    private static int placeholders(String s) {
        int n = 0;
        for (int i = s.indexOf("{0}"); i >= 0; i = s.indexOf("{0}", i + 1)) n++;
        return n;
    }

    @Test
    void everyLanguageHasEveryPhraseWithTheSamePlaceholders() {
        for (Language language : Language.values()) {
            for (Phrase phrase : Phrase.values()) {
                assertTrue(book.has(language, phrase), language + " is missing " + phrase);
                assertEquals(placeholders(book.template(Language.ENGLISH, phrase)),
                        placeholders(book.template(language, phrase)),
                        language + " " + phrase + " has a different number of {0}");
            }
        }
    }

    @Test
    void textReplacesPlaceholdersAndKeepsApostrophes() {
        assertEquals("Dear Meena,", book.text(Language.ENGLISH, Phrase.GREETING_FORMAL, "Meena"));
        assertTrue(book.text(Language.FRENCH, Phrase.LEAD_INVITATION_FORMAL).contains("l'événement"));
    }
}
