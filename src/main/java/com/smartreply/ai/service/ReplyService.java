package com.smartreply.ai.service;

import com.smartreply.ai.exception.ReplyNotFoundException;
import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.Reply;
import com.smartreply.ai.model.Tone;
import com.smartreply.ai.strategy.ReplyStrategy;
import com.smartreply.ai.strategy.ReplyStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates and manages {@link Reply} drafts.
 * <p>
 * OOP concept - Dependency Injection: EmailService, KeywordService and
 * ReplyStrategyFactory are all supplied by Spring through the constructor
 * rather than created with {@code new} inside this class, which keeps
 * ReplyService easy to unit test with fakes/mocks.
 * <p>
 * {@code @SessionScope} for the same reason as {@link EmailService}: the
 * generated-replies list and "time saved" counters are per-user state, not
 * shared across every signed-in account.
 */
@Service
@SessionScope
public class ReplyService {

    /** Rough average seconds a person takes to type a reply by hand - used only for the dashboard's "time saved" stat. */
    private static final int BASE_SECONDS_SAVED = 90;
    private static final int SECONDS_SAVED_PER_ACTION_ITEM = 20;

    private final EmailService emailService;
    private final KeywordService keywordService;
    private final ReplyStrategyFactory strategyFactory;
    private final LocalizedReplyComposer composer;

    private final List<Reply> replies = new ArrayList<>();
    private final AtomicInteger idSequence = new AtomicInteger();

    public ReplyService(EmailService emailService, KeywordService keywordService, ReplyStrategyFactory strategyFactory,
                        LocalizedReplyComposer composer) {
        this.emailService = emailService;
        this.keywordService = keywordService;
        this.strategyFactory = strategyFactory;
        this.composer = composer;
    }

    public Reply generate(String emailId, Tone tone) {
        return generate(emailId, tone, Language.ENGLISH, null, null);
    }

    /**
     * @param language   the language of the fixed wording (names, subjects and dates are copied as written)
     * @param extraNote  an optional line of the user's own, added just before the closing
     * @param senderName the signed-in user's name for the signature; blank keeps the demo signature
     */
    public Reply generate(String emailId, Tone tone, Language language, String extraNote, String senderName) {
        Language lang = language == null ? Language.ENGLISH : language;
        Email originalEmail = emailService.getById(emailId);
        KeywordAnalysis analysis = keywordService.analyze(originalEmail.getSubject(), originalEmail.getBody());
        String content;
        if (lang == Language.ENGLISH) {
            ReplyStrategy strategy = strategyFactory.getStrategy(tone);
            content = strategy.generate(originalEmail, analysis);
            if (senderName != null && !senderName.isBlank()) {
                content = content.replace(LocalizedReplyComposer.DEFAULT_SENDER, senderName.trim());
            }
        } else {
            content = composer.compose(lang, tone, originalEmail, analysis, senderName);
        }
        content = withExtraNote(content, extraNote);

        int secondsSaved = BASE_SECONDS_SAVED + analysis.getActionItems().size() * SECONDS_SAVED_PER_ACTION_ITEM;
        Reply reply = new Reply(nextId(), emailId, tone, content, LocalDateTime.now(), secondsSaved, lang);
        replies.add(reply);
        return reply;
    }

    /** Puts the user's own line between the body and the closing (the last paragraph). */
    private static String withExtraNote(String content, String extraNote) {
        if (extraNote == null || extraNote.isBlank()) {
            return content;
        }
        String note = extraNote.replaceAll("[\\p{Cntrl}]", " ").trim().replaceAll("\\s+", " ");
        int split = content.lastIndexOf("\n\n");
        if (split < 0) {
            return content + "\n\n" + note;
        }
        return content.substring(0, split) + "\n\n" + note + content.substring(split);
    }

    public Reply getById(String id) {
        return replies.stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ReplyNotFoundException(id));
    }

    public Reply updateContent(String id, String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("Reply content must not be empty.");
        }
        Reply reply = getById(id);
        reply.setContent(newContent);
        reply.setEdited(true);
        return reply;
    }

    public int getTotalRepliesGenerated() {
        return replies.size();
    }

    public int getTotalSecondsSaved() {
        return replies.stream().mapToInt(Reply::getEstimatedSecondsSaved).sum();
    }

    public void reset() {
        replies.clear();
        idSequence.set(0);
    }

    private String nextId() {
        return "r" + idSequence.incrementAndGet();
    }
}
