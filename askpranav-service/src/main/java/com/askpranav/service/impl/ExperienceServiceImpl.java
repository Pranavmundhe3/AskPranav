package com.askpranav.service.impl;

import com.askpranav.domain.Experience;
import com.askpranav.repository.ExperienceRepository;
import com.askpranav.service.ExperienceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExperienceServiceImpl implements ExperienceService {

    @Autowired
    private ExperienceRepository experienceRepository;

    @Override
    public List<Experience> getExperienceDetails() {
        return experienceRepository.findAll();
    }

    @Override
    public List<Experience> getExperienceByCompany(String company) {
        return experienceRepository.findByCompanyContainingIgnoreCase(company);
    }

    @Override
    public Experience saveExperience(Experience experience) {
        return experienceRepository.save(experience);
    }
}
