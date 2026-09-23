package com.askpranav.ai.tools.dto;

public record ProjectDto(
        String name,
        String duration,
        String shortDescription,
        String techStack,
        String githubUrl,
        String liveUrl,
        String readmeExcerpt
) {
}
