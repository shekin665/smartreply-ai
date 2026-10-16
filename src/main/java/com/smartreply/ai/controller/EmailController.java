package com.smartreply.ai.controller;

import com.smartreply.ai.dto.ComposeRequest;
import com.smartreply.ai.dto.DashboardStatsDto;
import com.smartreply.ai.dto.EmailDetailDto;
import com.smartreply.ai.dto.EmailSummaryDto;
import com.smartreply.ai.dto.SyncResultDto;
import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;
import com.smartreply.ai.model.KeywordAnalysis;
import com.smartreply.ai.model.SentEmail;
import com.smartreply.ai.service.EmailService;
import com.smartreply.ai.service.KeywordService;
import com.smartreply.ai.service.ReplyService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for listing/reading emails, folder actions (star, archive,
 * spam, restore, delete), composing/saving drafts, real Gmail sending and
 * real inbox sync. Authentication itself is enforced by Spring Security
 * (see {@link com.smartreply.ai.config.SecurityConfig}) before any of these
 * methods run - every request here is already a signed-in Google user.
 */
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailService emailService;
    private final KeywordService keywordService;
    private final ReplyService replyService;

    public EmailController(EmailService emailService, KeywordService keywordService, ReplyService replyService) {
        this.emailService = emailService;
        this.keywordService = keywordService;
        this.replyService = replyService;
    }

    @GetMapping
    public List<EmailSummaryDto> list(@RequestParam Folder folder, @RequestParam(required = false) String search) {
        List<Email> emails = emailService.search(folder, search);
        return emails.stream().map(EmailSummaryDto::from).toList();
    }

    @GetMapping("/{id}")
    public EmailDetailDto view(@PathVariable String id) {
        Email email = emailService.getById(id);
        if (!email.isRead()) {
            emailService.setRead(id, true);
        }
        return EmailDetailDto.from(email);
    }

    @GetMapping("/{id}/keywords")
    public KeywordAnalysis keywords(@PathVariable String id) {
        Email email = emailService.getById(id);
        return keywordService.analyze(email.getSubject(), email.getBody());
    }

    @PostMapping("/{id}/read")
    public EmailDetailDto setRead(@PathVariable String id, @RequestParam boolean read) {
        emailService.setRead(id, read);
        return EmailDetailDto.from(emailService.getById(id));
    }

    @PostMapping("/{id}/star")
    public EmailDetailDto toggleStar(@PathVariable String id) {
        emailService.toggleStar(id);
        return EmailDetailDto.from(emailService.getById(id));
    }

    @PostMapping("/{id}/archive")
    public EmailDetailDto archive(@PathVariable String id) {
        emailService.archive(id);
        return EmailDetailDto.from(emailService.getById(id));
    }

    @PostMapping("/{id}/spam")
    public EmailDetailDto markSpam(@PathVariable String id) {
        emailService.markAsSpam(id);
        return EmailDetailDto.from(emailService.getById(id));
    }

    @PostMapping("/{id}/restore")
    public EmailDetailDto restore(@PathVariable String id) {
        emailService.restore(id);
        return EmailDetailDto.from(emailService.getById(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        emailService.delete(id);
    }

    @PostMapping("/draft")
    public EmailDetailDto saveDraft(@Valid @RequestBody ComposeRequest request, @AuthenticationPrincipal OAuth2User principal) {
        SentEmail draft = emailService.saveDraft(email(principal), displayName(principal), request.getId(),
                request.getTo(), request.getSubject(), request.getBody(), request.getRepliedToEmailId());
        return EmailDetailDto.from(draft);
    }

    @PostMapping("/send")
    public EmailDetailDto send(@Valid @RequestBody ComposeRequest request,
                                @AuthenticationPrincipal OAuth2User principal,
                                @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        boolean generatedByAi = request.getRepliedToEmailId() != null;
        SentEmail email = emailService.send(accessToken(authorizedClient), email(principal), displayName(principal),
                request.getId(), request.getTo(), request.getSubject(), request.getBody(),
                request.getRepliedToEmailId(), generatedByAi);
        return EmailDetailDto.from(email);
    }

    @PostMapping("/sync-real-inbox")
    public SyncResultDto syncRealInbox(@AuthenticationPrincipal OAuth2User principal,
                                        @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient authorizedClient) {
        int imported = emailService.syncRealInbox(accessToken(authorizedClient), email(principal), 20);
        return new SyncResultDto(imported);
    }

    @GetMapping("/stats")
    public DashboardStatsDto stats() {
        return new DashboardStatsDto(
                emailService.getByFolder(Folder.INBOX).size(),
                emailService.countUnread(Folder.INBOX),
                emailService.getByFolder(Folder.SENT).size(),
                emailService.getByFolder(Folder.DRAFTS).size(),
                emailService.getByFolder(Folder.SPAM).size(),
                replyService.getTotalRepliesGenerated(),
                replyService.getTotalSecondsSaved() / 60
        );
    }

    private String email(OAuth2User principal) {
        return principal.getAttribute("email");
    }

    private String displayName(OAuth2User principal) {
        String name = principal.getAttribute("name");
        return name != null ? name : email(principal);
    }

    private String accessToken(OAuth2AuthorizedClient authorizedClient) {
        return authorizedClient.getAccessToken().getTokenValue();
    }
}
