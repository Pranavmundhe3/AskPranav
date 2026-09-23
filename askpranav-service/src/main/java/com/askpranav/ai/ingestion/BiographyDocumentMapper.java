package com.askpranav.ai.ingestion;

import com.askpranav.domain.Certifications;
import com.askpranav.domain.Education;
import com.askpranav.domain.Experience;
import com.askpranav.domain.Project;
import com.askpranav.domain.Publication;
import com.askpranav.domain.Skills;
import com.askpranav.domain.Summary;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Turns each of the structured bio/project rows into a Spring AI {@link Document} with metadata
 * that {@link com.askpranav.ai.tools.BiographyTools#matchJobDescription} and
 * {@link com.askpranav.ai.rag.RagQuestionService} use to label sources back to the user.
 */
@Component
public class BiographyDocumentMapper {

    public Document map(Summary summary) {
        return Document.builder()
                .text("Career summary: " + summary.getSummaryDetails())
                .metadata(Map.of("source", "jpa-entity", "entityType", "summary", "label", "Career Summary"))
                .build();
    }

    public Document map(Experience experience) {
        String text = "Experience: %s at %s%s, %s (%s). %s".formatted(
                experience.getPosition(),
                experience.getCompany(),
                experience.getClient() != null && !experience.getClient().isBlank() ? " (client: " + experience.getClient() + ")" : "",
                experience.getLocation(),
                experience.getYear(),
                experience.getDescription());
        return Document.builder()
                .text(text)
                .metadata(Map.of("source", "jpa-entity", "entityType", "experience", "entityId", experience.getId(),
                        "label", experience.getPosition() + " @ " + experience.getCompany()))
                .build();
    }

    public Document map(Education education) {
        String text = "Education: %s from %s (%s), grades: %s".formatted(
                education.getDegree(), education.getUniversity(), education.getYearOfCompletion(), education.getGrades());
        return Document.builder()
                .text(text)
                .metadata(Map.of("source", "jpa-entity", "entityType", "education", "entityId", education.getId(),
                        "label", education.getDegree()))
                .build();
    }

    public Document map(Skills skills) {
        String text = "Skill category %s: %s".formatted(skills.getType(), skills.getName());
        return Document.builder()
                .text(text)
                .metadata(Map.of("source", "jpa-entity", "entityType", "skill", "entityId", skills.getId(),
                        "label", skills.getType()))
                .build();
    }

    public Document map(Certifications certification) {
        String text = "Certification: %s, completed %s".formatted(certification.getName(), certification.getCompletedOn());
        return Document.builder()
                .text(text)
                .metadata(Map.of("source", "jpa-entity", "entityType", "certification", "entityId", certification.getId(),
                        "label", certification.getName()))
                .build();
    }

    public Document map(Project project) {
        String text = "Project: %s%s. %s Tech stack: %s. %s".formatted(
                project.getName(),
                project.getDuration() != null && !project.getDuration().isBlank() ? " (" + project.getDuration() + ")" : "",
                project.getShortDescription() != null ? project.getShortDescription() : "",
                project.getTechStack() != null ? project.getTechStack() : "",
                project.getReadmeExcerpt() != null ? project.getReadmeExcerpt() : "");
        return Document.builder()
                .text(text)
                .metadata(Map.of("source", "jpa-entity", "entityType", "project", "entityId", project.getId(),
                        "label", project.getName()))
                .build();
    }

    public Document map(Publication publication) {
        String text = "Publication: %s (%s), published in %s. %s Technologies: %s".formatted(
                publication.getTitle(),
                publication.getPublishedOn(),
                publication.getVenue(),
                publication.getDescription() != null ? publication.getDescription() : "",
                publication.getTechnologies() != null ? publication.getTechnologies() : "");
        return Document.builder()
                .text(text)
                .metadata(Map.of("source", "jpa-entity", "entityType", "publication", "entityId", publication.getId(),
                        "label", publication.getTitle()))
                .build();
    }

    public List<Document> mapAll(List<Summary> summaries, List<Experience> experiences, List<Education> educations,
                                  List<Skills> skills, List<Certifications> certifications, List<Project> projects,
                                  List<Publication> publications) {
        return java.util.stream.Stream.of(
                        summaries.stream().map(this::map),
                        experiences.stream().map(this::map),
                        educations.stream().map(this::map),
                        skills.stream().map(this::map),
                        certifications.stream().map(this::map),
                        projects.stream().map(this::map),
                        publications.stream().map(this::map))
                .flatMap(s -> s)
                .toList();
    }
}
