package com.smartreply.ai.controller;

import com.smartreply.ai.dto.ReplyDto;
import com.smartreply.ai.dto.ReplyGenerateRequest;
import com.smartreply.ai.dto.ReplyUpdateRequest;
import com.smartreply.ai.model.Reply;
import com.smartreply.ai.service.ReplyService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for generating and editing simulated AI replies. Like
 * every other {@code /api/**} controller, authentication is enforced by
 * Spring Security before these methods run.
 */
@RestController
@RequestMapping("/api/replies")
public class ReplyController {

    private final ReplyService replyService;

    public ReplyController(ReplyService replyService) {
        this.replyService = replyService;
    }

    @PostMapping("/generate")
    public ReplyDto generate(@Valid @RequestBody ReplyGenerateRequest request,
                             @AuthenticationPrincipal OAuth2User user) {
        Reply reply = replyService.generate(request.getEmailId(), request.getTone(), request.getLanguage(),
                request.getExtraNote(), senderName(user));
        return ReplyDto.from(reply);
    }

    /** The signed-in user's name for the signature, or null when there is none (then the demo name is used). */
    public static String senderName(OAuth2User user) {
        if (user == null) {
            return null;
        }
        Object name = user.getAttributes().get("name");
        return name == null ? null : name.toString();
    }

    @GetMapping("/{id}")
    public ReplyDto get(@PathVariable String id) {
        return ReplyDto.from(replyService.getById(id));
    }

    @PutMapping("/{id}")
    public ReplyDto update(@PathVariable String id, @Valid @RequestBody ReplyUpdateRequest request) {
        return ReplyDto.from(replyService.updateContent(id, request.getContent()));
    }
}
