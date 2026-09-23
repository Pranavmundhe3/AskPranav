package com.askpranav.service.impl;

import com.askpranav.domain.Certifications;
import com.askpranav.repository.CertificationRepository;
import com.askpranav.service.CertificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CertificationServiceImpl implements CertificationService {

    @Autowired
    private CertificationRepository certificationRepository;

    @Override
    public List<Certifications> getCertificationDetails() {
        return certificationRepository.findAll();
    }

    @Override
    public Certifications saveCertification(Certifications certification) {
        return certificationRepository.save(certification);
    }
}
