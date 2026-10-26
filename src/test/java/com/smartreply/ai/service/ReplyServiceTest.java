package com.smartreply.ai.service;

import com.smartreply.ai.config.DemoDataSeeder;
import com.smartreply.ai.exception.ReplyNotFoundException;
import com.smartreply.ai.model.Email;
import com.smartreply.ai.i18n.PhraseBook;
import com.smartreply.ai.model.Folder;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.Reply;
import com.smartreply.ai.model.Tone;
import com.smartreply.ai.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReplyServiceTest {

    private EmailService emailService;
    private ReplyService replyService;

    @BeforeEach
    void setUp() {
        // Both gateway interfaces have a single abstract method, so a no-op lambda stands in fine here -
        // ReplyService never triggers a send/sync itself, it only reads emails through EmailService.
        emailService = new EmailService(new DemoDataSeeder(),
                (accessToken, fromAddress, fromName, to, subject, body) -> { },
                (accessToken, currentUserAddress, limit) -> List.of());
        KeywordService keywordService = new KeywordService();
        ReplyStrategyFactory factory = new ReplyStrategyFactory(List.of(
                new ProfessionalReplyStrategy(),
                new FriendlyReplyStrategy(),
                new FormalReplyStrategy(),
                new ConciseReplyStrategy(),
                new ApologeticReplyStrategy(),
                new DetailedReplyStrategy()
        ));
        replyService = new ReplyService(emailService, keywordService, factory,
                new LocalizedReplyComposer(new PhraseBook()));
    }

    @Test
    void generateProducesNonBlankContentForEveryTone() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        for (Tone tone : Tone.values()) {
            Reply reply = replyService.generate(email.getId(), tone);
            assertEquals(tone, reply.getTone());
            assertFalse(reply.getContent().isBlank());
            assertEquals(email.getId(), reply.getOriginalEmailId());
        }
    }

    @Test
    void formalToneUsesFormalGreeting() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        Reply reply = replyService.generate(email.getId(), Tone.FORMAL);
        assertTrue(reply.getContent().startsWith("Dear "));
    }

    @Test
    void generateIncrementsDashboardCounters() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        assertEquals(0, replyService.getTotalRepliesGenerated());

        replyService.generate(email.getId(), Tone.CONCISE);

        assertEquals(1, replyService.getTotalRepliesGenerated());
        assertTrue(replyService.getTotalSecondsSaved() > 0);
    }

    @Test
    void updateContentMarksReplyAsEdited() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        Reply reply = replyService.generate(email.getId(), Tone.FRIENDLY);

        Reply updated = replyService.updateContent(reply.getId(), "Edited by the user.");

        assertTrue(updated.isEdited());
        assertEquals("Edited by the user.", updated.getContent());
    }

    @Test
    void getByIdThrowsForUnknownReply() {
        assertThrows(ReplyNotFoundException.class, () -> replyService.getById("missing"));
    }

    @Test
    void signatureUsesTheSignedInNameAndKeepsTheDemoNameOtherwise() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        String named = replyService.generate(email.getId(), Tone.FORMAL, Language.ENGLISH, null, "Shekin").getContent();
        assertTrue(named.endsWith("Shekin"));
        assertFalse(named.contains("Demo User"));
        assertTrue(replyService.generate(email.getId(), Tone.FORMAL).getContent().endsWith("SmartReply AI (Demo User)"));
    }

    @Test
    void extraNoteSitsJustBeforeTheClosing() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        for (Language language : new Language[]{Language.ENGLISH, Language.TAMIL}) {
            String content = replyService.generate(email.getId(), Tone.FRIENDLY, language, "See you at 5.", "Shekin").getContent();
            String[] parts = content.split("\n\n");
            assertEquals("See you at 5.", parts[parts.length - 2]);
            assertTrue(parts[parts.length - 1].endsWith("Shekin"));
        }
    }

    @Test
    void otherLanguagesUseTheirOwnWordingAndRecordTheLanguage() {
        Email email = emailService.getByFolder(Folder.INBOX).get(0);
        Reply tamil = replyService.generate(email.getId(), Tone.FORMAL, Language.TAMIL, null, null);
        assertEquals(Language.TAMIL, tamil.getLanguage());
        assertFalse(tamil.getContent().contains("Dear"));
        assertTrue(tamil.getContent().contains("வணக்கம்") || tamil.getContent().contains("அன்புள்ள"));
        Reply french = replyService.generate(email.getId(), Tone.APOLOGETIC, Language.FRENCH, null, null);
        assertTrue(french.getContent().contains("excuses") || french.getContent().contains("excuser"));
        for (Language language : Language.values()) {
            for (Tone tone : Tone.values()) {
                assertFalse(replyService.generate(email.getId(), tone, language, null, null).getContent().isBlank());
            }
        }
    }
}
