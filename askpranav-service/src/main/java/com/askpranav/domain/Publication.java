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
 * New vertical (not present in the source Biography-service, and distinct from {@link Project}).
 * A publication is externally reviewed/published work - a journal or conference paper - and carries
 * venue/issue metadata a personal engineering project has no use for, so it gets its own schema
 * rather than being shoehorned into the Project table.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "PUBLICATION")
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "PUBLISHED_ON")
    private String publishedOn;

    @Column(name = "VENUE", length = 500)
    private String venue;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "TECHNOLOGIES", length = 500)
    private String technologies;

    @Column(name = "URL")
    private String url;
}
