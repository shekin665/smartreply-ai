package com.smartreply.ai.controller;

import com.smartreply.ai.service.PriorityKeywordService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * The signed-in user's personal priority keywords. Every endpoint returns the full,
 * updated list so the dashboard can redraw straight from the response.
 */
@RestController
@RequestMapping("/api/priority-keywords")
public class PriorityKeywordController {

    private final PriorityKeywordService priorityKeywordService;

    public PriorityKeywordController(PriorityKeywordService priorityKeywordService) {
        this.priorityKeywordService = priorityKeywordService;
    }

    @GetMapping
    public Map<String, List<String>> list() {
        return wrap(priorityKeywordService.getKeywords());
    }

    @PostMapping
    public Map<String, List<String>> add(@RequestBody Map<String, String> body) {
        return wrap(priorityKeywordService.add(body.get("keyword")));
    }

    /** Replaces the whole list. The dashboard uses this to restore the copy saved in the browser. */
    @PutMapping
    public Map<String, List<String>> replace(@RequestBody Map<String, List<String>> body) {
        return wrap(priorityKeywordService.replaceAll(body.get("keywords")));
    }

    @DeleteMapping
    public Map<String, List<String>> remove(@RequestParam String keyword) {
        return wrap(priorityKeywordService.remove(keyword));
    }

    private Map<String, List<String>> wrap(List<String> keywords) {
        return Map.of("keywords", keywords);
    }
}
