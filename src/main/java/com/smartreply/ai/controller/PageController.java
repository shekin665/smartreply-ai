package com.smartreply.ai.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Thymeleaf page controller - only responsible for deciding which HTML
 * shell to render (login vs dashboard). Authentication itself is Spring
 * Security's job (see {@link com.smartreply.ai.config.SecurityConfig}): by
 * the time a request reaches {@code dashboard()}, the user is guaranteed to
 * already be signed in with Google, or Spring Security would have redirected
 * them to {@code /login} first. All dynamic data on those pages is fetched
 * afterwards by static/js/app.js calling the REST controllers.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String root(@AuthenticationPrincipal OAuth2User principal) {
        return principal != null ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal OAuth2User principal) {
        return principal != null ? "redirect:/dashboard" : "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        String name = principal.getAttribute("name");
        String email = principal.getAttribute("email");
        model.addAttribute("displayName", name != null ? name : email);
        return "dashboard";
    }
}
