package com.askpranav.ai.tools;

import com.askpranav.ai.tools.dto.CertificationDto;
import com.askpranav.ai.tools.dto.ContactInfoDto;
import com.askpranav.ai.tools.dto.EducationDto;
import com.askpranav.ai.tools.dto.ExperienceDto;
import com.askpranav.ai.tools.dto.JobMatchEvidenceDto;
import com.askpranav.ai.tools.dto.ProjectDto;
import com.askpranav.ai.tools.dto.SkillDto;
import com.askpranav.domain.Personal;
import com.askpranav.domain.Project;
import com.askpranav.service.CertificationService;
import com.askpranav.service.EducationService;
import com.askpranav.service.ExperienceService;
import com.askpranav.service.PersonalService;
import com.askpranav.service.ProjectService;
import com.askpranav.service.SkillService;
import com.askpranav.service.SummaryService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Every method here is exposed two ways at once: as a Spring AI {@code @Tool} the ChatClient can
 * call mid-conversation, AND (via {@link com.askpranav.ai.config.McpToolConfig}) as an MCP tool any
 * MCP client - Claude Desktop, Claude Code, anything speaking the protocol - can call directly.
 * Methods return DTOs, never raw JPA entities, so the LLM/MCP boundary never leaks persistence
 * details and never lets a tool call accidentally trigger lazy-loading.
 */
@Component
public class BiographyTools {

    @Autowired
    private PersonalService personalService;
    @Autowired
    private EducationService educationService;
    @Autowired
    private ExperienceService experienceService;
    @Autowired
    private SkillService skillService;
    @Autowired
    private CertificationService certificationService;
    @Autowired
    private SummaryService summaryService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private VectorStore vectorStore;

    @Tool(description = "Get Pranav's professional summary / elevator pitch.")
    public String getCareerSummary() {
        var summary = summaryService.getSummaryDetails();
        return summary != null ? summary.getSummaryDetails() : "No summary on file.";
    }

    @Tool(description = "List all of Pranav's work experience entries, optionally filtered by company name.")
    public List<ExperienceDto> getExperience(
            @ToolParam(description = "Optional company name to filter by. Leave blank for all experience.", required = false)
            String company) {
        var experiences = (company == null || company.isBlank())
                ? experienceService.getExperienceDetails()
                : experienceService.getExperienceByCompany(company);
        return experiences.stream()
                .map(e -> new ExperienceDto(e.getPosition(), e.getCompany(), e.getClient(), e.getLocation(), e.getYear(), e.getDescription()))
                .toList();
    }

    @Tool(description = "List all of Pranav's technical and professional skills, optionally filtered by skill type/category.")
    public List<SkillDto> getSkills(
            @ToolParam(description = "Optional skill type/category to filter by (e.g. Frameworks, Tools). Leave blank for all skills.", required = false)
            String type) {
        var skills = (type == null || type.isBlank())
                ? skillService.getSkillDetails()
                : skillService.getSkillsByType(type);
        return skills.stream().map(s -> new SkillDto(s.getName(), s.getType())).toList();
    }

    @Tool(description = "List Pranav's educational background.")
    public List<EducationDto> getEducation() {
        return educationService.getEducationDetails().stream()
                .map(e -> new EducationDto(e.getDegree(), e.getUniversity(), e.getYearOfCompletion(), e.getGrades()))
                .toList();
    }

    @Tool(description = "List all of Pranav's professional certifications.")
    public List<CertificationDto> getCertifications() {
        return certificationService.getCertificationDetails().stream()
                .map(c -> new CertificationDto(c.getName(), c.getCompletedOn()))
                .toList();
    }

    @Tool(description = "Get details about one of Pranav's projects by name, including tech stack and GitHub link.")
    public ProjectDto getProjectDetails(
            @ToolParam(description = "Project name, or a partial/fuzzy match of it")
            String name) {
        Optional<Project> project = projectService.getProjectByName(name);
        return project.map(p -> new ProjectDto(p.getName(), p.getShortDescription(), p.getTechStack(), p.getGithubUrl(), p.getLiveUrl(), p.getReadmeExcerpt()))
                .orElse(null);
    }

    @Tool(description = "Get Pranav's contact information: email, LinkedIn, GitHub, and portfolio links.")
    public ContactInfoDto getContactInfo() {
        Personal personal = personalService.getPersonalDetails();
        if (personal == null) {
            return null;
        }
        return new ContactInfoDto(personal.getEmail(), personal.getLinkedinUrl(), personal.getGithubUrl(), personal.getPortfolioUrl());
    }

    @Tool(description = """
            Paste in a job description and get back grounded evidence of how Pranav's real experience,
            skills, and projects relate to it - pulled from his actual resume/project/README data via
            semantic search, not guessed. Use this whenever a user pastes or describes a job posting
            and asks whether/how Pranav fits it.""")
    public JobMatchEvidenceDto matchJobDescription(
            @ToolParam(description = "The full or partial text of the job description to match against")
            String jobDescriptionText) {
        // VERIFY: SearchRequest builder shape (topK/similarityThreshold names) against the
        // Spring AI version actually pinned in pom.xml - this API has moved across milestones.
        SearchRequest request = SearchRequest.builder()
                .query(jobDescriptionText)
                .topK(8)
                .build();
        List<Document> matches = vectorStore.similaritySearch(request);

        List<JobMatchEvidenceDto.MatchedSnippet> snippets = matches.stream()
                .map(doc -> new JobMatchEvidenceDto.MatchedSnippet(
                        String.valueOf(doc.getMetadata().getOrDefault("source", "unknown")),
                        String.valueOf(doc.getMetadata().getOrDefault("label", doc.getId())),
                        doc.getText()))
                .toList();

        String excerpt = jobDescriptionText.length() > 300
                ? jobDescriptionText.substring(0, 300) + "..."
                : jobDescriptionText;
        return new JobMatchEvidenceDto(excerpt, snippets);
    }
}
