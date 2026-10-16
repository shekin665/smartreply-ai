package com.smartreply.ai.controller;

import com.smartreply.ai.dto.ReplyDto;
import com.smartreply.ai.dto.ReplyGenerateRequest;
import com.smartreply.ai.dto.ReplyUpdateRequest;
import com.smartreply.ai.model.Reply;
import com.smartreply.ai.service.ReplyService;
import jakarta.validation.Valid;
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
    public ReplyDto generate(@Valid @RequestBody ReplyGenerateRequest request) {
        Reply reply = replyService.generate(request.getEmailId(), request.getTone());
        return ReplyDto.from(reply);
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
