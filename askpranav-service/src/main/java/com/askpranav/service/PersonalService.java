package com.askpranav.service;

import com.askpranav.domain.Personal;

public interface PersonalService {

    Personal getPersonalDetails();

    Personal savePersonal(Personal personal);
}
