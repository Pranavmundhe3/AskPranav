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

/**
 * New vertical (not present in the source Biography-service). Backs the {@code getProjectDetails}
 * tool and gives RAG ingestion something to embed beyond the 6 original single-row bio entities.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PROJECT")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "SHORT_DESCRIPTION", length = 500)
    private String shortDescription;

    @Column(name = "TECH_STACK", length = 500)
    private String techStack;

    @Column(name = "GITHUB_URL")
    private String githubUrl;

    @Column(name = "LIVE_URL")
    private String liveUrl;

    @Column(name = "README_EXCERPT", length = 4000)
    private String readmeExcerpt;
}
