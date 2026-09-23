package com.askpranav.service.impl;

import com.askpranav.domain.Skills;
import com.askpranav.repository.SkillRepository;
import com.askpranav.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkillServiceImpl implements SkillService {

    @Autowired
    private SkillRepository skillRepository;

    @Override
    public List<Skills> getSkillDetails() {
        return skillRepository.findAll();
    }

    @Override
    public List<Skills> getSkillsByType(String type) {
        return skillRepository.findByTypeContainingIgnoreCase(type);
    }

    @Override
    public Skills saveSkill(Skills skill) {
        return skillRepository.save(skill);
    }
}
