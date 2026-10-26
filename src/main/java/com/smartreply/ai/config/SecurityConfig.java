package com.smartreply.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

/**
 * Every SmartReply AI user is now a real Google account - there is no demo
 * login. Spring Security handles the whole OAuth2 "Sign in with Google" flow
 * (redirect to Google, consent screen, callback, session creation); the app
 * itself never sees or stores a password.
 * <p>
 * Two choices worth calling out for the viva:
 * <ul>
 *   <li>{@code /api/**} gets its own 401 JSON entry point, while every other
 *       unauthenticated request still redirects to {@code /login} - a
 *       {@code fetch()} call from app.js should see a clean 401 it can show
 *       as a toast, but a plain browser navigation to {@code /dashboard}
 *       should behave like a normal web page. Both are wired explicitly into
 *       one {@link DelegatingAuthenticationEntryPoint} below, rather than
 *       relying on {@code defaultAuthenticationEntryPointFor} with a single
 *       mapping - with only one mapping that method quietly makes its entry
 *       point the *global* default, which would have broken the browser
 *       redirect for every page, not just the API.</li>
 *   <li>CSRF protection is left ON (cookie-based, readable by JavaScript via
 *       {@link CookieCsrfTokenRepository#withHttpOnlyFalse()}) rather than
 *       disabled, because this app can now send real email - a forged
 *       cross-site POST to {@code /api/emails/send} from another tab would
 *       otherwise be able to send mail as the logged-in user.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login")
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                );
        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        RequestMatcher apiMatcher = new AntPathRequestMatcher("/api/**");
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> mapping = new LinkedHashMap<>();
        mapping.put(apiMatcher, jsonUnauthorizedEntryPoint());

        DelegatingAuthenticationEntryPoint delegate = new DelegatingAuthenticationEntryPoint(mapping);
        delegate.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
        return delegate;
    }

    private AuthenticationEntryPoint jsonUnauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Sign in with Google first.\"}");
        };
    }
}
