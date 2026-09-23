package com.askpranav.service;

import com.askpranav.domain.Skills;

import java.util.List;

public interface SkillService {

    List<Skills> getSkillDetails();

    List<Skills> getSkillsByType(String type);

    Skills saveSkill(Skills skill);
}
