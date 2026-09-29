package com.askpranav.config;

import com.askpranav.domain.Project;
import com.askpranav.repository.CertificationRepository;
import com.askpranav.repository.EducationRepository;
import com.askpranav.repository.ExperienceRepository;
import com.askpranav.repository.PersonalRepository;
import com.askpranav.repository.ProjectRepository;
import com.askpranav.repository.PublicationRepository;
import com.askpranav.repository.SkillRepository;
import com.askpranav.repository.SummaryRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the two things this class must get right, since it is destructive: it only clears when asked,
 * and it never clears projects even when it does.
 */
class DataSeederTest {

    private final PersonalRepository personals = mock(PersonalRepository.class);
    private final EducationRepository educations = mock(EducationRepository.class);
    private final ExperienceRepository experiences = mock(ExperienceRepository.class);
    private final SkillRepository skills = mock(SkillRepository.class);
    private final CertificationRepository certifications = mock(CertificationRepository.class);
    private final SummaryRepository summaries = mock(SummaryRepository.class);
    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final PublicationRepository publications = mock(PublicationRepository.class);

    private DataSeeder seeder(boolean reseed) {
        return new DataSeeder(personals, educations, experiences, skills, certifications, summaries,
                projects, publications, reseed);
    }

    @Test
    void byDefaultNothingIsClearedEvenIfEveryTableAlreadyHasRows() {
        when(personals.count()).thenReturn(1L);
        when(educations.count()).thenReturn(1L);
        when(experiences.count()).thenReturn(1L);
        when(skills.count()).thenReturn(1L);
        when(certifications.count()).thenReturn(1L);
        when(summaries.count()).thenReturn(1L);
        when(publications.count()).thenReturn(1L);

        seeder(false).run();

        verify(personals, never()).deleteAllInBatch();
        verify(experiences, never()).deleteAllInBatch();
        verify(skills, never()).deleteAllInBatch();
    }

    @Test
    void reseedClearsEveryOwnedTableButNeverProjects() {
        seeder(true).run();

        verify(personals).deleteAllInBatch();
        verify(educations).deleteAllInBatch();
        verify(experiences).deleteAllInBatch();
        verify(skills).deleteAllInBatch();
        verify(certifications).deleteAllInBatch();
        verify(summaries).deleteAllInBatch();
        verify(publications).deleteAllInBatch();
        verify(projects, never()).deleteAllInBatch();
        verify(projects, never()).deleteAll();
    }

    @Test
    void reseedActuallyReinsertsAfterClearing() {
        seeder(true).run();

        // count() still reports 0 post-clear (these are mocks; nothing changes their default 0 return),
        // so every seedX() proceeds to insert - this is what makes the resync actually take effect.
        verify(personals, times(1)).save(any());
        verify(summaries, times(1)).save(any());
        verify(experiences, times(3)).save(any());
    }

    @Test
    void anExistingProjectMatchedByNameIsUpdatedInPlaceNotDuplicated() {
        Project existing = new Project();
        existing.setId(7L);
        existing.setName("AskPranav - AI Career Agent");
        existing.setLiveUrl("https://kept.example"); // a field DataSeeder does not set - must survive
        when(projects.findFirstByNameContainingIgnoreCase("AskPranav - AI Career Agent"))
                .thenReturn(Optional.of(existing));
        when(projects.findFirstByNameContainingIgnoreCase("AI-Powered Job Search Agent"))
                .thenReturn(Optional.empty());

        seeder(false).run();

        assertThat(existing.getId()).isEqualTo(7L);
        assertThat(existing.getLiveUrl()).isEqualTo("https://kept.example");
        assertThat(existing.getShortDescription()).contains("AI agent that answers recruiter questions");
        verify(projects, never()).deleteAll();
        verify(projects, never()).deleteAllInBatch();
    }
}
