package com.askpranav.service;

import com.askpranav.domain.Experience;

import java.util.List;

public interface ExperienceService {

    List<Experience> getExperienceDetails();

    List<Experience> getExperienceByCompany(String company);

    Experience saveExperience(Experience experience);
}
