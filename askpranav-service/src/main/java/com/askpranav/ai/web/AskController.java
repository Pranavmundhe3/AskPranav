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
        AnswerResponse response = orchestrationService.answer(request.question());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
