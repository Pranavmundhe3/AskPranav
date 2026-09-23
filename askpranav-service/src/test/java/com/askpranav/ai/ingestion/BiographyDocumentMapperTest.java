package com.askpranav.ai.ingestion;

import com.askpranav.domain.Experience;
import com.askpranav.domain.Skills;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

class BiographyDocumentMapperTest {

    private final BiographyDocumentMapper mapper = new BiographyDocumentMapper();

    @Test
    void mapsExperienceToDocumentWithSourceMetadata() {
        Experience experience = new Experience(1L, "Software Developer", "Mumbai, India",
                "LTIMindtree", "Nets DK", "Aug 2020 - Present", "Payment gateway development.");

        Document document = mapper.map(experience);

        assertThat(document.getText()).contains("Software Developer").contains("LTIMindtree").contains("Nets DK");
        assertThat(document.getMetadata()).containsEntry("source", "jpa-entity");
        assertThat(document.getMetadata()).containsEntry("entityType", "experience");
        assertThat(document.getMetadata()).containsEntry("entityId", 1L);
    }

    @Test
    void mapsSkillsToDocumentWithTypeAsLabel() {
        Skills skills = new Skills(2L, "Java, Spring Boot", "Frameworks");

        Document document = mapper.map(skills);

        assertThat(document.getText()).contains("Frameworks").contains("Java, Spring Boot");
        assertThat(document.getMetadata()).containsEntry("label", "Frameworks");
    }
}
