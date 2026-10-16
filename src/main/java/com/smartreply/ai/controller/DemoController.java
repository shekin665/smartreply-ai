package com.smartreply.ai.controller;

import com.smartreply.ai.service.EmailService;
import com.smartreply.ai.service.ReplyService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets the presenter reset the mailbox to its original seeded state mid-demo. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final EmailService emailService;
    private final ReplyService replyService;

    public DemoController(EmailService emailService, ReplyService replyService) {
        this.emailService = emailService;
        this.replyService = replyService;
    }

    @PostMapping("/reset")
    public void reset() {
        emailService.reset();
        replyService.reset();
    }
}
