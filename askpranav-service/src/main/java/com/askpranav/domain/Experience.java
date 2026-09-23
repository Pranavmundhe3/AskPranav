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
@Table(name = "EXPERIENCE")
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "POSITION")
    private String position;

    @Column(name = "LOCATION")
    private String location;

    @Column(name = "COMPANY")
    private String company;

    @Column(name = "CLIENT")
    private String client;

    @Column(name = "YEAR")
    private String year;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;
}
