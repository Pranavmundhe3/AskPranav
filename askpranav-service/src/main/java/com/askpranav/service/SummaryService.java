package com.askpranav.service;

import com.askpranav.domain.Summary;

public interface SummaryService {

    Summary getSummaryDetails();

    Summary saveSummary(Summary summary);
}
