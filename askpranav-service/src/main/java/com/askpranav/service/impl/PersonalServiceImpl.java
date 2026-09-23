package com.askpranav.service.impl;

import com.askpranav.domain.Personal;
import com.askpranav.repository.PersonalRepository;
import com.askpranav.service.PersonalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonalServiceImpl implements PersonalService {

    @Autowired
    private PersonalRepository personalRepository;

    @Override
    public Personal getPersonalDetails() {
        // Personal is genuinely a single row by design (one person). Take the first row that
        // exists rather than hardcoding id=1, so the app doesn't break if the row is re-seeded
        // with a different generated id.
        return personalRepository.findAll().stream().findFirst().orElse(null);
    }

    @Override
    public Personal savePersonal(Personal personal) {
        return personalRepository.save(personal);
    }
}
