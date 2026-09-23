package com.askpranav.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PERSONAL")
public class Personal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DOB")
    private String dob;

    @Column(name = "HOBBIES")
    private String hobbies;

    @Column(name = "LANGUAGES")
    private String languages;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "LINKEDIN_URL")
    private String linkedinUrl;

    @Column(name = "GITHUB_URL")
    private String githubUrl;

    @Column(name = "PORTFOLIO_URL")
    private String portfolioUrl;
}
