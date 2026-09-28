package com.askpranav.ai.web;

import com.askpranav.ai.orchestration.OrchestrationService;
import com.askpranav.ai.rag.AnswerResponse;
import lombok.extern.slf4j.Slf4j;
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

    public AskController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/question")
    public ResponseEntity<AnswerResponse> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().build();
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
}
