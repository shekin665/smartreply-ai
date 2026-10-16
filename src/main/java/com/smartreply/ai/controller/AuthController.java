package com.smartreply.ai.controller;

import com.smartreply.ai.dto.CurrentUserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The actual sign-in/sign-out flow is handled entirely by Spring Security's
 * OAuth2 login ({@code /oauth2/authorization/google}, callback, logout) -
 * this controller only exposes who is currently signed in, read from the
 * Google-issued {@link OAuth2User} principal Spring Security populates.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public CurrentUserDto me(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String pictureUrl = principal.getAttribute("picture");
        return new CurrentUserDto(email, name != null ? name : email, pictureUrl);
    }
}
