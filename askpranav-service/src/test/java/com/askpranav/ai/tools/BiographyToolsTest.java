package com.askpranav.ai.tools;

import com.askpranav.ai.tools.dto.ContactInfoDto;
import com.askpranav.ai.tools.dto.ExperienceDto;
import com.askpranav.domain.Experience;
import com.askpranav.domain.Personal;
import com.askpranav.service.CertificationService;
import com.askpranav.service.EducationService;
import com.askpranav.service.ExperienceService;
import com.askpranav.service.PersonalService;
import com.askpranav.service.ProjectService;
import com.askpranav.service.SkillService;
import com.askpranav.service.SummaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Exercises the DTO-mapping logic in BiographyTools against mocked services - no real database,
 * vector store, or LLM call involved, so this runs free and fast in CI.
 */
@ExtendWith(MockitoExtension.class)
class BiographyToolsTest {

    @Mock
    private PersonalService personalService;
    @Mock
    private EducationService educationService;
    @Mock
    private ExperienceService experienceService;
    @Mock
    private SkillService skillService;
    @Mock
    private CertificationService certificationService;
    @Mock
    private SummaryService summaryService;
    @Mock
    private ProjectService projectService;
    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private BiographyTools biographyTools;

    @Test
    void getExperienceMapsEntitiesToDtosWithoutFilter() {
        Experience experience = new Experience(1L, "Software Developer", "Mumbai, India",
                "LTIMindtree", "Nets DK", "Aug 2020 - Present", "Payment gateways.");
        when(experienceService.getExperienceDetails()).thenReturn(List.of(experience));

        List<ExperienceDto> result = biographyTools.getExperience(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).company()).isEqualTo("LTIMindtree");
        assertThat(result.get(0).position()).isEqualTo("Software Developer");
    }

    @Test
    void getExperienceFiltersByCompanyWhenProvided() {
        Experience experience = new Experience(1L, "Software Developer", "Mumbai, India",
                "LTIMindtree", "Nets DK", "Aug 2020 - Present", "Payment gateways.");
        when(experienceService.getExperienceByCompany("LTIMindtree")).thenReturn(List.of(experience));

        List<ExperienceDto> result = biographyTools.getExperience("LTIMindtree");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).company()).isEqualTo("LTIMindtree");
    }

    @Test
    void getContactInfoReturnsNullWhenNoPersonalRecordExists() {
        when(personalService.getPersonalDetails()).thenReturn(null);

        ContactInfoDto result = biographyTools.getContactInfo();

        assertThat(result).isNull();
    }

    @Test
    void getContactInfoMapsPersonalFields() {
        Personal personal = new Personal(1L, "1 January 2000", "Swimming", "English",
                "pranav@example.com", "linkedin.com/in/pranav", "github.com/pranav", "pranav.dev");
        when(personalService.getPersonalDetails()).thenReturn(personal);

        ContactInfoDto result = biographyTools.getContactInfo();

        assertThat(result.email()).isEqualTo("pranav@example.com");
        assertThat(result.githubUrl()).isEqualTo("github.com/pranav");
    }
}
