package com.askpranav.service;

import com.askpranav.domain.Certifications;

import java.util.List;

public interface CertificationService {

    List<Certifications> getCertificationDetails();

    Certifications saveCertification(Certifications certification);
}
