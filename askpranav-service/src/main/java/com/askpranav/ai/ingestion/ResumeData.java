package com.askpranav.ai.ingestion;

import java.util.List;

/**
 * What the model extracts from a resume document. Every field is optional on purpose: the model is told to
 * leave out anything the document does not state, and {@link ResumeImportService} decides whether what
 * came back is complete enough to trust.
 */
public record ResumeData(
        String summary,
        List<Role> experience,
        List<Degree> education,
        List<SkillGroup> skills,
        List<Certificate> certifications,
        List<String> languages,
        List<ProjectItem> projects,
        List<PublicationItem> publications
) {

    /** One job. {@code bullets} are the achievement points, one string per point. */
    public record Role(String title, String company, String client, String location, String period,
                       List<String> bullets) {
    }

    public record Degree(String degree, String institution, String period, String grade) {
    }

    /** A skills category (e.g. "Databases") and its comma-separated skills. */
    public record SkillGroup(String category, String skills) {
    }

    public record Certificate(String name, String completedOn) {
    }

    public record ProjectItem(String name, String period, String description, String techStack) {
    }

    public record PublicationItem(String title, String publishedOn, String venue, String description,
                                  String technologies) {
    }
}
