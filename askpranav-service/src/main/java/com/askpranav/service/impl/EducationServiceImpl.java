package com.askpranav.service.impl;

import com.askpranav.domain.Education;
import com.askpranav.repository.EducationRepository;
import com.askpranav.service.EducationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EducationServiceImpl implements EducationService {

    @Autowired
    private EducationRepository educationRepository;

    @Override
    public List<Education> getEducationDetails() {
        return educationRepository.findAll();
    }

    @Override
    public Education saveEducation(Education education) {
        return educationRepository.save(education);
    }
}
