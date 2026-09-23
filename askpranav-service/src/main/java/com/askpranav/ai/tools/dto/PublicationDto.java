package com.askpranav.ai.tools.dto;

public record PublicationDto(
        String title,
        String publishedOn,
        String venue,
        String description,
        String technologies,
        String url
) {
}
