package com.askpranav.ai.ingestion;

import com.askpranav.domain.Certifications;
import com.askpranav.domain.Education;
import com.askpranav.domain.Experience;
import com.askpranav.domain.Personal;
import com.askpranav.domain.Project;
import com.askpranav.domain.Skills;
import com.askpranav.domain.Summary;
import com.askpranav.repository.CertificationRepository;
import com.askpranav.repository.EducationRepository;
import com.askpranav.repository.ExperienceRepository;
import com.askpranav.repository.PersonalRepository;
import com.askpranav.repository.ProjectRepository;
import com.askpranav.repository.PublicationRepository;
import com.askpranav.repository.SkillRepository;
import com.askpranav.repository.SummaryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResumeImportServiceTest {

    private final SummaryRepository summaries = mock(SummaryRepository.class);
    private final ExperienceRepository experiences = mock(ExperienceRepository.class);
    private final EducationRepository educations = mock(EducationRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final CertificationRepository certifications = mock(CertificationRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final PublicationRepository publications = mock(PublicationRepository.class);
    private final PersonalRepository personals = mock(PersonalRepository.class);

    private final ResumeImportService service = new ResumeImportService(
            summaries, experiences, educations, skills, certifications, projects, publications, personals);

    private static ResumeData.Role role(String title, String company, String client, String... bullets) {
        return new ResumeData.Role(title, company, client, null, "2020 - 2023", List.of(bullets));
    }

    private static ResumeData resume(List<ResumeData.Role> roles) {
        return new ResumeData("A summary.", roles, null, null, null, null, null, null);
    }

    @SuppressWarnings("unchecked")
    private List<Experience> savedExperience() {
        ArgumentCaptor<List<Experience>> captor = ArgumentCaptor.forClass((Class<List<Experience>>) (Class<?>) List.class);
        verify(experiences).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void replacesExperienceKeepsBulletsAsLinesAndStripsBulletCharacters() {
        service.apply(resume(List.of(role("Engineer", "Acme", null, "• Built X", "  - Cut Y by 30%  ", ""))));

        verify(experiences).deleteAllInBatch();
        List<Experience> saved = savedExperience();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getDescription()).isEqualTo("Built X\nCut Y by 30%");
        assertThat(saved.get(0).getYear()).isEqualTo("2020 - 2023");
    }

    @Test
    void keepsClientAndLocationWhenTheResumeDoesNotStateThem() {
        Experience old = new Experience(1L, "Engineer", "Acme", "Acme", "Volkswagen", "2020", "old");
        old.setLocation("Mumbai");
        when(experiences.findAll()).thenReturn(List.of(old));

        service.apply(resume(List.of(role("Engineer", "acme", null, "Did things"))));

        Experience saved = savedExperience().get(0);
        assertThat(saved.getClient()).isEqualTo("Volkswagen");
        assertThat(saved.getLocation()).isEqualTo("Mumbai");
    }

    @Test
    void anIncompleteExtractionChangesNothing() {
        ResumeData noExperience = new ResumeData("A summary.", List.of(), null, null, null, null, null, null);
        ResumeData noSummary = new ResumeData(" ", List.of(role("E", "C", null, "x")), null, null, null, null, null, null);
        ResumeData roleWithoutBullets = resume(List.of(role("E", "C", null)));

        for (ResumeData bad : new ResumeData[] {noExperience, noSummary, roleWithoutBullets, null}) {
            assertThatThrownBy(() -> service.apply(bad)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(summaries, experiences, educations, skills, certifications, projects, publications, personals);
    }

    @Test
    void keepsCertificationDatesAndGradesTheResumeLeavesOut() {
        when(certifications.findAll()).thenReturn(List.of(new Certifications(1L, "May 2022", "AWS Certified Cloud Practitioner")));
        when(educations.findAll()).thenReturn(List.of(new Education(1L, "B.E.", "University of Mumbai, India", "2020", "8.69 / 10")));
        ResumeData data = new ResumeData("S.", List.of(role("E", "C", null, "x")),
                List.of(new ResumeData.Degree("B.E. IT", "University of Mumbai, India", "2016 - 2020", null)),
                null,
                List.of(new ResumeData.Certificate("aws certified cloud practitioner", null)),
                null, null, null);

        service.apply(data);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Certifications>> certs = ArgumentCaptor.forClass((Class<List<Certifications>>) (Class<?>) List.class);
        verify(certifications).saveAll(certs.capture());
        assertThat(certs.getValue().get(0).getCompletedOn()).isEqualTo("May 2022");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Education>> edu = ArgumentCaptor.forClass((Class<List<Education>>) (Class<?>) List.class);
        verify(educations).saveAll(edu.capture());
        assertThat(edu.getValue().get(0).getGrades()).isEqualTo("8.69 / 10");
    }

    @Test
    void projectsAreUpdatedByNameNeverDeleted() {
        Project existing = new Project();
        existing.setName("AI-Powered Job Search Agent");
        existing.setGithubUrl("https://github.com/example/repo");
        when(projects.findFirstByNameContainingIgnoreCase("AI-Powered Job Search Agent")).thenReturn(Optional.of(existing));
        ResumeData data = new ResumeData("S.", List.of(role("E", "C", null, "x")), null, null, null, null,
                List.of(new ResumeData.ProjectItem("AI-Powered Job Search Agent", "2026 - Present", "New text", "Python")),
                null);

        service.apply(data);

        assertThat(existing.getShortDescription()).isEqualTo("New text");
        assertThat(existing.getGithubUrl()).isEqualTo("https://github.com/example/repo");
        verify(projects).save(existing);
        verify(projects, never()).deleteAll();
        verify(projects, never()).deleteAllInBatch();
    }

    @Test
    void onlyLanguagesOfThePersonalRowChange() {
        Personal personal = new Personal();
        personal.setHobbies("Music");
        personal.setEmail("me@example.com");
        when(personals.findAll()).thenReturn(List.of(personal));
        ResumeData data = new ResumeData("S.", List.of(role("E", "C", null, "x")), null, null, null,
                List.of("English (C1)", "German (A2)"), null, null);

        service.apply(data);

        assertThat(personal.getLanguages()).isEqualTo("English (C1), German (A2)");
        assertThat(personal.getHobbies()).isEqualTo("Music");
        assertThat(personal.getEmail()).isEqualTo("me@example.com");
        verify(personals).save(personal);
    }

    @Test
    void summaryRowIsUpdatedInPlace() {
        Summary existing = new Summary();
        existing.setId(7L);
        when(summaries.findAll()).thenReturn(List.of(existing));

        service.apply(resume(List.of(role("E", "C", null, "x"))));

        assertThat(existing.getSummaryDetails()).isEqualTo("A summary.");
        verify(summaries).save(any(Summary.class));
    }

    @Test
    void skillGroupsReplaceTheOldOnes() {
        ResumeData data = new ResumeData("S.", List.of(role("E", "C", null, "x")), null,
                List.of(new ResumeData.SkillGroup("Databases", "Oracle SQL, PostgreSQL"), new ResumeData.SkillGroup(" ", "ignored")),
                null, null, null, null);

        service.apply(data);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Skills>> captor = ArgumentCaptor.forClass((Class<List<Skills>>) (Class<?>) List.class);
        verify(skills).deleteAllInBatch();
        verify(skills).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getType()).isEqualTo("Databases");
    }
}
