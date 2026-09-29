package com.askpranav.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * Write protection, default-deny, plus login on the MCP endpoints. Reads stay public (this is a public
 * resume site), and so do the two POSTs visitors legitimately use anonymously: the chat and the contact
 * form. Every other write - including every {@code POST /...save-...} endpoint and any that gets added
 * later - needs the admin login (HTTP Basic). The MCP endpoints ({@code GET /sse}, {@code POST
 * /mcp/message}) are read-only (they only call the same {@code @Tool} methods the chat already exposes)
 * but are metered/costed (Gemini calls) and were previously reachable by anyone who found the URL, so
 * they need their own login too - either the MCP account or the admin account.
 *
 * Both passwords come only from configuration ({@code ASKPRANAV_ADMIN_PASSWORD}, {@code
 * ASKPRANAV_MCP_PASSWORD}). If one is not set, that account simply does not exist, so the endpoints it
 * would guard reject every request rather than being protected by a guessable default.
 *
 * CSRF protection is off on purpose: authentication is a per-request Authorization header, with no cookie
 * or session for a forged cross-site request to ride on.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Uses the @CrossOrigin settings on the controllers, and answers preflights before auth runs.
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // More specific matchers first: Spring Security uses the first match, and these two
                        // MCP paths would otherwise fall under the permitAll GET/POST rules below.
                        .requestMatchers(HttpMethod.GET, "/sse", "/sse/**").hasAnyRole("MCP", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/mcp/message", "/mcp/message/**").hasAnyRole("MCP", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/ask/question", "/contact/send").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${askpranav.security.admin-username:admin}") String adminUsername,
            @Value("${askpranav.security.admin-password:}") String adminPassword,
            @Value("${askpranav.security.mcp-username:mcp}") String mcpUsername,
            @Value("${askpranav.security.mcp-password:}") String mcpPassword) {
        List<UserDetails> accounts = new ArrayList<>();

        if (isBlank(adminPassword)) {
            log.warn("ASKPRANAV_ADMIN_PASSWORD is not set: no admin account exists, so all write endpoints "
                    + "(POST .../save-*) will reject every request.");
        } else {
            accounts.add(User.withUsername(adminUsername).password(passwordEncoder.encode(adminPassword))
                    .roles("ADMIN").build());
        }

        if (isBlank(mcpPassword)) {
            log.warn("ASKPRANAV_MCP_PASSWORD is not set: no MCP account exists, so the MCP endpoints "
                    + "(GET /sse, POST /mcp/message) will reject every request. The admin account can still "
                    + "reach them.");
        } else {
            accounts.add(User.withUsername(mcpUsername).password(passwordEncoder.encode(mcpPassword))
                    .roles("MCP").build());
        }

        return new InMemoryUserDetailsManager(accounts);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
