package com.smartreply.ai.service;

import com.smartreply.ai.dto.MailGenerateRequest;
import com.smartreply.ai.i18n.PhraseBook;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.MailKind;
import com.smartreply.ai.model.Tone;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MailGeneratorServiceTest {

    private final MailGeneratorService service = new MailGeneratorService(new PhraseBook());

    private static MailGenerateRequest request() {
        MailGenerateRequest r = new MailGenerateRequest();
        r.setKind(MailKind.INVITATION);
        r.setEventTitle("Annual Tech Fest");
        r.setDate(LocalDate.of(2026, 10, 30)); // a Friday
        r.setTime(LocalTime.of(10, 30));
        r.setVenue("Main Auditorium");
        r.setKeyPoints("Bring your ID card\n- Lunch provided; Dress code: smart casual");
        r.setRecipientName("Meena");
        return r;
    }

    @Test
    void formalEnglishMailHasAllTheDetails() {
        var mail = service.generate(request(), "Shekin");
        assertEquals("Invitation: Annual Tech Fest – 30 Oct 2026", mail.subject());
        String body = mail.body();
        assertTrue(body.startsWith("Dear Meena,"));
        assertTrue(body.contains("Event: Annual Tech Fest"));
        assertTrue(body.contains("Date: Friday, 30 October 2026"));
        assertTrue(body.contains("Time: 10:30 AM"));
        assertTrue(body.contains("Venue: Main Auditorium"));
        assertTrue(body.contains("• Bring your ID card\n• Lunch provided\n• Dress code: smart casual"));
        assertTrue(body.endsWith("Yours sincerely,\nShekin"));
    }

    @Test
    void conciseStyleIsShortAndDetailedAddsAnExtraLine() {
        MailGenerateRequest r = request();
        r.setTone(Tone.CONCISE);
        String concise = service.generate(r, null).body();
        assertTrue(concise.contains("30 Oct 2026 | 10:30 AM | Main Auditorium"));
        assertTrue(concise.contains("Bring your ID card; Lunch provided; Dress code: smart casual"));
        assertFalse(concise.contains("Event:"));
        r.setTone(Tone.DETAILED);
        String detailed = service.generate(r, null).body();
        assertTrue(detailed.contains("further details"));
        assertTrue(detailed.length() > concise.length());
    }

    @Test
    void tamilMailUsesTamilWordingAndKeepsTypedTextAsWritten() {
        MailGenerateRequest r = request();
        r.setLanguage(Language.TAMIL);
        var mail = service.generate(r, "Shekin");
        assertTrue(mail.subject().startsWith("அழைப்பிதழ்: Annual Tech Fest"));
        assertTrue(mail.body().startsWith("அன்புள்ள Meena,"));
        assertTrue(mail.body().contains("Annual Tech Fest"));
        assertFalse(mail.body().contains("Dear"));
        assertFalse(mail.body().contains(" "));
    }

    @Test
    void everyLanguageKindAndStyleProducesAMail() {
        for (Language language : Language.values()) {
            for (MailKind kind : MailKind.values()) {
                for (Tone tone : Tone.values()) {
                    MailGenerateRequest r = request();
                    r.setLanguage(language);
                    r.setKind(kind);
                    r.setTone(tone);
                    var mail = service.generate(r, "Shekin");
                    assertFalse(mail.subject().isBlank());
                    assertTrue(mail.body().contains("Annual Tech Fest"));
                    assertFalse(mail.body().contains("{0}"));
                }
            }
        }
    }

    @Test
    void withoutRecipientTheGreetingHasNoName() {
        MailGenerateRequest r = request();
        r.setRecipientName("  ");
        r.setTime(null);
        r.setVenue(null);
        r.setKeyPoints(null);
        String body = service.generate(r, null).body();
        assertTrue(body.startsWith("Hello,"));
        assertFalse(body.contains("Time:"));
        assertFalse(body.contains("Venue:"));
        assertFalse(body.contains("Key points"));
        assertTrue(body.endsWith(LocalizedReplyComposer.DEFAULT_SENDER));
    }

    @Test
    void keyPointsAreSplitAndCapped() {
        assertEquals(List.of("a", "b", "c"), MailGeneratorService.splitPoints("a;b\n\n  • c  ;"));
        String many = String.join(";", java.util.Collections.nCopies(20, "x"));
        assertEquals(MailGeneratorService.MAX_KEY_POINTS, MailGeneratorService.splitPoints(many).size());
        assertEquals(MailGeneratorService.MAX_POINT_LENGTH, MailGeneratorService.splitPoints("y".repeat(500)).get(0).length());
    }

    @Test
    void missingTitleOrDateIsRejected() {
        MailGenerateRequest r = request();
        r.setEventTitle("   ");
        assertThrows(IllegalArgumentException.class, () -> service.generate(r, null));
        MailGenerateRequest r2 = request();
        r2.setDate(null);
        assertThrows(IllegalArgumentException.class, () -> service.generate(r2, null));
    }
}
