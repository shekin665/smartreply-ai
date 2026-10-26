package com.smartreply.ai.service;

import com.smartreply.ai.exception.TextAssistException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextAssistServiceTest {

    @Test
    void tidyFixesCapitalsSpacingShortFormsAndTypos() {
        assertEquals("Hi Meena,\nPlease come tomorrow, i will be there.".replace("i will", "I will"),
                TextCleaner.tidy("hi meena,\npls  come tomorow ,i will be there"));
        assertEquals("I think I am done.", TextCleaner.tidy("i think i am done"));
        assertEquals("Thank you for your help.", TextCleaner.tidy("thank u for ur help"));
    }

    @Test
    void tidyKeepsShortLinesAndLinksAlone() {
        assertEquals("Regards,\nShekin", TextCleaner.tidy("Regards,\nShekin"));
        assertEquals("https://example.com/a?b=c", TextCleaner.tidy("https://example.com/a?b=c"));
    }

    @Test
    void formalExpandsContractionsAndGreeting() {
        String t = TextCleaner.formal("hi meena,\ni can't come and i'm busy. thanks");
        assertTrue(t.startsWith("Dear Meena,"));
        assertTrue(t.contains("cannot come"));
        assertTrue(t.contains("I am busy"));
        assertTrue(t.endsWith("Thank you."));
    }

    @Test
    void shorterDropsFillerWords() {
        assertEquals("I wanted to ask about the exam.", TextCleaner.shorter("i just really wanted to ask about the exam"));
    }

    @Test
    void longLinesAreSplitOnSentenceEnds() {
        String line = "First sentence is here. " + "word ".repeat(100) + "end.";
        List<String> parts = TextSplitter.split(line, 450);
        assertTrue(parts.size() >= 2);
        for (String p : parts) assertTrue(p.length() <= 450);
        assertEquals("First sentence is here.", parts.get(0).substring(0, 23));
    }

    @Test
    void translationReplyIsCheckedForLimitsAndEntities() {
        assertEquals("It's \"ok\"", TextAssistService.parseTranslation("It&#39;s &quot;ok&quot;"));
        assertThrows(TextAssistException.class, () -> TextAssistService.parseTranslation(""));
        assertThrows(TextAssistException.class,
                () -> TextAssistService.parseTranslation("MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS FOR TODAY."));
        assertTrue(TextAssistService.looksUntranslatable("https://x.com/a"));
        assertTrue(TextAssistService.looksUntranslatable("See https://x.com/a"));
        assertTrue(TextAssistService.looksUntranslatable("-----"));
        assertFalse(TextAssistService.looksUntranslatable("See you tomorrow"));
    }
}
