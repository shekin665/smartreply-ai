package com.smartreply.ai.controller;

import com.smartreply.ai.dto.MailGenerateRequest;
import com.smartreply.ai.model.Language;
import com.smartreply.ai.model.MailKind;
import com.smartreply.ai.service.MailGeneratorService;
import com.smartreply.ai.service.MailGeneratorService.GeneratedMail;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Writes a mail from event details, and lists the languages and kinds the dashboard can offer. */
@RestController
@RequestMapping("/api/mail")
public class MailGeneratorController {

    public record Option(String value, String label) { }

    private final MailGeneratorService mailGeneratorService;

    public MailGeneratorController(MailGeneratorService mailGeneratorService) {
        this.mailGeneratorService = mailGeneratorService;
    }

    @PostMapping("/generate")
    public GeneratedMail generate(@Valid @RequestBody MailGenerateRequest request,
                                  @AuthenticationPrincipal OAuth2User user) {
        return mailGeneratorService.generate(request, ReplyController.senderName(user));
    }

    @GetMapping("/options")
    public Map<String, List<Option>> options() {
        return Map.of(
                "languages", Arrays.stream(Language.values()).map(l -> new Option(l.name(), l.getLabel())).toList(),
                "kinds", Arrays.stream(MailKind.values()).map(k -> new Option(k.name(), k.getLabel())).toList());
    }
}
