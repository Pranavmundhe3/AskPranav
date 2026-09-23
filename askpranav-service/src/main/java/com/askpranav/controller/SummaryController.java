package com.askpranav.controller;

import com.askpranav.domain.Summary;
import com.askpranav.service.SummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "${askpranav.cors.allowed-origin:http://localhost:4200}", allowedHeaders = "*")
@RestController
@RequestMapping("/summary")
@Slf4j
public class SummaryController {

    @Autowired
    private SummaryService summaryService;

    @GetMapping(value = "/summary-details")
    public ResponseEntity<Summary> getSummary() {
        log.info("Get Summary details Controller");
        return new ResponseEntity<>(summaryService.getSummaryDetails(), HttpStatus.OK);
    }

    @PostMapping(value = "/save-summary")
    public ResponseEntity<Summary> saveSummary(@RequestBody Summary summary) {
        return new ResponseEntity<>(summaryService.saveSummary(summary), HttpStatus.CREATED);
    }
}
