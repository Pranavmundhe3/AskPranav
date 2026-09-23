package com.askpranav.service;

import com.askpranav.domain.Education;

import java.util.List;

public interface EducationService {

    List<Education> getEducationDetails();

    Education saveEducation(Education education);
}
