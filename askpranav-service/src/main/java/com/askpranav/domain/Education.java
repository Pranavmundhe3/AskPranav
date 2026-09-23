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
@Table(name = "EDUCATION")
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "DEGREE")
    private String degree;

    @Column(name = "UNIVERSITY")
    private String university;

    @Column(name = "YEAR_OF_COMPLETION")
    private String yearOfCompletion;

    @Column(name = "GRADES")
    private String grades;
}
