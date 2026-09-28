package com.askpranav.ai.web;

import com.askpranav.ai.orchestration.OrchestrationService;
import com.askpranav.ai.rag.AnswerResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The single AI-facing HTTP entry point. Routes every question through the Planner -> Retriever/
 * ToolCaller -> Writer pipeline in {@link OrchestrationService}. The same capabilities are also
 * reachable directly, tool-by-tool, over MCP (see {@link com.askpranav.ai.config.McpToolConfig}) -
 * this endpoint is for callers that just want a single grounded natural-language answer.
 */
@CrossOrigin(origins = "${askpranav.cors.allowed-origin:http://localhost:4200}", allowedHeaders = "*")
@RestController
@RequestMapping("/ask")
@Slf4j
public class AskController {

    private final OrchestrationService orchestrationService;
    private final AskRateLimiter rateLimiter;

    public AskController(OrchestrationService orchestrationService, AskRateLimiter rateLimiter) {
        this.orchestrationService = orchestrationService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/question")
    public ResponseEntity<AnswerResponse> ask(@RequestBody AskRequest request, HttpServletRequest http) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // Checked after validation so a malformed request doesn't burn one of the caller's questions.
        // Behind a reverse proxy, set server.forward-headers-strategy=native (ASKPRANAV_FORWARD_HEADERS)
        // so this is the visitor's IP rather than the proxy's.
        AskRateLimiter.Decision decision = rateLimiter.tryAcquire(http.getRemoteAddr());
        if (!decision.allowed()) {
            log.info("Rate limit hit; retry in {}s", decision.retryAfterSeconds());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()))
                    .body(new AnswerResponse(rateLimitMessage(decision.retryAfterSeconds()), List.of(), "RATE_LIMITED"));
        }
        log.info("AskPranav question received");
        try {
            return new ResponseEntity<>(orchestrationService.answer(request.question()), HttpStatus.OK);
        } catch (RuntimeException e) {
            // Provider-side failures (rate limits, overload) surface as RuntimeExceptions from the model
            // client; report them as a temporary outage rather than an opaque 500.
            log.warn("Model call failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AnswerResponse(
                            "The language model is temporarily unavailable or rate limited. Please try again shortly.",
                            List.of(), "ERROR"));
        }
    }

    private String rateLimitMessage(long retryAfterSeconds) {
        long windowMinutes = Math.max(1, rateLimiter.windowSeconds() / 60);
        String wait = retryAfterSeconds >= 60
                ? ((retryAfterSeconds + 59) / 60) + " minute(s)"
                : retryAfterSeconds + " second(s)";
        return "To keep this free to use, each visitor can ask " + rateLimiter.maxRequests()
                + " questions every " + windowMinutes + " minutes. Please try again in " + wait + ".";
    }
}
