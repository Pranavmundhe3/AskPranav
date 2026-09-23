package com.askpranav.service.impl;

import com.askpranav.domain.Summary;
import com.askpranav.repository.SummaryRepository;
import com.askpranav.service.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SummaryServiceImpl implements SummaryService {

    @Autowired
    private SummaryRepository summaryRepository;

    @Override
    public Summary getSummaryDetails() {
        return summaryRepository.findAll().stream().findFirst().orElse(null);
    }

    @Override
    public Summary saveSummary(Summary summary) {
        return summaryRepository.save(summary);
    }
}
