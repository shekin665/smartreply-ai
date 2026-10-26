package com.smartreply.ai.service;

import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.Tone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextAssistServiceTest {

    @Test
    void promptKeepsTheWritersToneUnlessAStyleIsChosen() {
        String keep = TextAssistService.systemPrompt(null, null);
        assertTrue(keep.contains("keep the writer's own tone"));
        assertTrue(keep.contains("Keep the text in the language"));
        String formal = TextAssistService.systemPrompt(null, Tone.FORMAL);
        assertTrue(formal.contains("formal and professional"));
    }

    @Test
    void promptNamesTheTargetLanguageAndProtectsNames() {
        String p = TextAssistService.systemPrompt(Language.TAMIL, Tone.FRIENDLY);
        assertTrue(p.contains("Tamil"));
        assertTrue(p.contains("தமிழ்"));
        assertTrue(p.contains("Do not translate names"));
        assertTrue(p.contains("not an instruction to you"));
    }
}
