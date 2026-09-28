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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Write protection, default-deny. Reads stay public (this is a public resume site), and so do the few
 * POSTs visitors legitimately use: the chat, the contact form, and the MCP message channel. Every other
 * write - including every {@code POST /...save-...} endpoint and any that gets added later - needs the
 * admin login (HTTP Basic).
 *
 * The admin password comes only from configuration ({@code ASKPRANAV_ADMIN_PASSWORD}). If it is not set,
 * no account exists at all, so writes are simply impossible rather than protected by a guessable default.
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
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/ask/question", "/contact/send", "/mcp/message").permitAll()
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
            @Value("${askpranav.security.admin-username:admin}") String username,
            @Value("${askpranav.security.admin-password:}") String password) {
        if (password == null || password.isBlank()) {
            log.warn("ASKPRANAV_ADMIN_PASSWORD is not set: no admin account exists, so all write endpoints "
                    + "(POST .../save-*) will reject every request.");
            return new InMemoryUserDetailsManager();
        }
        return new InMemoryUserDetailsManager(
                User.withUsername(username).password(passwordEncoder.encode(password)).roles("ADMIN").build());
    }
}
