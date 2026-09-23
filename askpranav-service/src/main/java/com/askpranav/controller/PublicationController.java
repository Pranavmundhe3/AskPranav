package com.askpranav.controller;

import com.askpranav.domain.Publication;
import com.askpranav.service.PublicationService;
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

import java.util.List;

@CrossOrigin(origins = "${askpranav.cors.allowed-origin:http://localhost:4200}", allowedHeaders = "*")
@RestController
@RequestMapping("/publication")
@Slf4j
public class PublicationController {

    @Autowired
    private PublicationService publicationService;

    @GetMapping(value = "/publication-details")
    public ResponseEntity<List<Publication>> getPublicationDetails() {
        log.info("Get Publication details Controller");
        return new ResponseEntity<>(publicationService.getPublicationDetails(), HttpStatus.OK);
    }

    @PostMapping(value = "/save-publication")
    public ResponseEntity<Publication> savePublication(@RequestBody Publication publication) {
        return new ResponseEntity<>(publicationService.savePublication(publication), HttpStatus.CREATED);
    }
}
