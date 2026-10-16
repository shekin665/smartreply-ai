package com.smartreply.ai.service;

import com.smartreply.ai.config.DemoDataSeeder;
import com.smartreply.ai.exception.ReplyNotFoundException;
import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;
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
                new ApologeticReplyStrategy()
        ));
        replyService = new ReplyService(emailService, keywordService, factory);
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
}
