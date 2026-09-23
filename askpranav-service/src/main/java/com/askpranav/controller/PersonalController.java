package com.askpranav.controller;

import com.askpranav.domain.Personal;
import com.askpranav.service.PersonalService;
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
@RequestMapping("/personal")
@Slf4j
public class PersonalController {

    @Autowired
    private PersonalService personalService;

    @GetMapping(value = "/personal-details")
    public ResponseEntity<Personal> getPersonalDetails() {
        log.info("Get Personal details Controller");
        return new ResponseEntity<>(personalService.getPersonalDetails(), HttpStatus.OK);
    }

    @PostMapping(value = "/save-personal")
    public ResponseEntity<Personal> savePersonal(@RequestBody Personal personal) {
        return new ResponseEntity<>(personalService.savePersonal(personal), HttpStatus.CREATED);
    }
}
