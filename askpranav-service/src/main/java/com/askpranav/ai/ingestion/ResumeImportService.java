package com.askpranav.ai.ingestion;

import com.askpranav.domain.Certifications;
import com.askpranav.domain.Education;
import com.askpranav.domain.Experience;
import com.askpranav.domain.Personal;
import com.askpranav.domain.Project;
import com.askpranav.domain.Publication;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Writes a resume the model has extracted into the tables the site reads.
 *
 * <p>Safety, since model output is not always right: the data is validated <em>before</em> anything is
 * touched, and the whole write is one transaction, so a bad or partial extraction leaves the existing rows
 * exactly as they were. Summary, experience, education, skills, certifications and publications are
 * replaced (the resume is their source of truth); projects are added or updated by name but never deleted,
 * because some projects (like this application) are not on the resume; hobbies, email and links are
 * never touched. Values the resume does not state (a client name, a certification date, a grade) are kept
 * from the existing row rather than blanked.
 */
@Service
public class ResumeImportService {

    private static final Logger log = LoggerFactory.getLogger(ResumeImportService.class);

    private static final int SUMMARY_MAX = 1000;
    private static final int DESCRIPTION_MAX = 2000;
    private static final int SKILLS_MAX = 500;
    private static final int SHORT_MAX = 255;

    private final SummaryRepository summaryRepository;
    private final ExperienceRepository experienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final ProjectRepository projectRepository;
    private final PublicationRepository publicationRepository;
    private final PersonalRepository personalRepository;

    public ResumeImportService(SummaryRepository summaryRepository,
                               ExperienceRepository experienceRepository,
                               EducationRepository educationRepository,
                               SkillRepository skillRepository,
                               CertificationRepository certificationRepository,
                               ProjectRepository projectRepository,
                               PublicationRepository publicationRepository,
                               PersonalRepository personalRepository) {
        this.summaryRepository = summaryRepository;
        this.experienceRepository = experienceRepository;
        this.educationRepository = educationRepository;
        this.skillRepository = skillRepository;
        this.certificationRepository = certificationRepository;
        this.projectRepository = projectRepository;
        this.publicationRepository = publicationRepository;
        this.personalRepository = personalRepository;
    }

    @Transactional
    public void apply(ResumeData data) {
        validate(data);

        replaceSummary(data.summary());
        replaceExperience(data.experience());
        if (present(data.education())) {
            replaceEducation(data.education());
        }
        if (present(data.skills())) {
            replaceSkills(data.skills());
        }
        if (present(data.certifications())) {
            replaceCertifications(data.certifications());
        }
        if (present(data.publications())) {
            replacePublications(data.publications());
        }
        if (present(data.projects())) {
            upsertProjects(data.projects());
        }
        if (present(data.languages())) {
            updateLanguages(data.languages());
        }
        log.info("Resume imported: {} role(s), {} skill group(s), {} certification(s), {} project(s).",
                data.experience().size(), size(data.skills()), size(data.certifications()), size(data.projects()));
    }

    /** Rejects an extraction that is too incomplete to trust. Throws before any row is changed. */
    static void validate(ResumeData data) {
        if (data == null) {
            throw new IllegalArgumentException("The extraction returned nothing.");
        }
        if (isBlank(data.summary())) {
            throw new IllegalArgumentException("The extraction has no professional summary.");
        }
        if (!present(data.experience())) {
            throw new IllegalArgumentException("The extraction found no work experience.");
        }
        for (ResumeData.Role role : data.experience()) {
            if (role == null || isBlank(role.title()) || isBlank(role.company())) {
                throw new IllegalArgumentException("A work experience entry is missing its title or company.");
            }
            if (cleanBullets(role.bullets()).isEmpty()) {
                throw new IllegalArgumentException("Work experience at " + role.company() + " has no bullet points.");
            }
        }
    }

    private void replaceSummary(String text) {
        Summary summary = summaryRepository.findAll().stream().findFirst().orElseGet(Summary::new);
        summary.setSummaryDetails(cut(text, SUMMARY_MAX));
        summaryRepository.save(summary);
    }

    private void replaceExperience(List<ResumeData.Role> roles) {
        List<Experience> existing = experienceRepository.findAll();
        List<Experience> replacement = new ArrayList<>();
        for (ResumeData.Role role : roles) {
            Optional<Experience> previous = matchingExperience(existing, role);
            Experience experience = new Experience();
            experience.setPosition(cut(role.title(), SHORT_MAX));
            experience.setCompany(cut(role.company(), SHORT_MAX));
            experience.setClient(firstNonBlank(cut(role.client(), SHORT_MAX), previous.map(Experience::getClient).orElse(null)));
            experience.setLocation(firstNonBlank(cut(role.location(), SHORT_MAX), previous.map(Experience::getLocation).orElse(null)));
            experience.setYear(cut(role.period(), SHORT_MAX));
            experience.setDescription(cutAtLine(String.join("\n", cleanBullets(role.bullets())), DESCRIPTION_MAX));
            replacement.add(experience);
        }
        experienceRepository.deleteAllInBatch();
        experienceRepository.saveAll(replacement);
    }

    private void replaceEducation(List<ResumeData.Degree> degrees) {
        List<Education> existing = educationRepository.findAll();
        List<Education> replacement = new ArrayList<>();
        for (ResumeData.Degree degree : degrees) {
            if (degree == null || isBlank(degree.degree())) {
                continue;
            }
            String previousGrade = existing.stream()
                    .filter(e -> sameText(e.getUniversity(), degree.institution()))
                    .map(Education::getGrades).filter(g -> !isBlank(g)).findFirst().orElse(null);
            Education education = new Education();
            education.setDegree(cut(degree.degree(), SHORT_MAX));
            education.setUniversity(cut(degree.institution(), SHORT_MAX));
            education.setYearOfCompletion(cut(degree.period(), SHORT_MAX));
            education.setGrades(firstNonBlank(cut(degree.grade(), SHORT_MAX), previousGrade));
            replacement.add(education);
        }
        if (replacement.isEmpty()) {
            return;
        }
        educationRepository.deleteAllInBatch();
        educationRepository.saveAll(replacement);
    }

    private void replaceSkills(List<ResumeData.SkillGroup> groups) {
        List<Skills> replacement = new ArrayList<>();
        for (ResumeData.SkillGroup group : groups) {
            if (group == null || isBlank(group.category()) || isBlank(group.skills())) {
                continue;
            }
            replacement.add(new Skills(null, cut(group.skills(), SKILLS_MAX), cut(group.category(), SHORT_MAX)));
        }
        if (replacement.isEmpty()) {
            return;
        }
        skillRepository.deleteAllInBatch();
        skillRepository.saveAll(replacement);
    }

    private void replaceCertifications(List<ResumeData.Certificate> certificates) {
        List<Certifications> existing = certificationRepository.findAll();
        List<Certifications> replacement = new ArrayList<>();
        for (ResumeData.Certificate certificate : certificates) {
            if (certificate == null || isBlank(certificate.name())) {
                continue;
            }
            String previousDate = existing.stream()
                    .filter(c -> sameText(c.getName(), certificate.name()))
                    .map(Certifications::getCompletedOn).filter(d -> !isBlank(d)).findFirst().orElse(null);
            replacement.add(new Certifications(null,
                    firstNonBlank(cut(certificate.completedOn(), SHORT_MAX), previousDate),
                    cut(certificate.name(), SHORT_MAX)));
        }
        if (replacement.isEmpty()) {
            return;
        }
        certificationRepository.deleteAllInBatch();
        certificationRepository.saveAll(replacement);
    }

    private void replacePublications(List<ResumeData.PublicationItem> items) {
        List<Publication> replacement = new ArrayList<>();
        for (ResumeData.PublicationItem item : items) {
            if (item == null || isBlank(item.title())) {
                continue;
            }
            Publication publication = new Publication();
            publication.setTitle(cut(item.title(), SHORT_MAX));
            publication.setPublishedOn(cut(item.publishedOn(), SHORT_MAX));
            publication.setVenue(cut(item.venue(), 500));
            publication.setDescription(cut(item.description(), 1000));
            publication.setTechnologies(cut(item.technologies(), 500));
            replacement.add(publication);
        }
        if (replacement.isEmpty()) {
            return;
        }
        publicationRepository.deleteAllInBatch();
        publicationRepository.saveAll(replacement);
    }

    private void upsertProjects(List<ResumeData.ProjectItem> items) {
        for (ResumeData.ProjectItem item : items) {
            if (item == null || isBlank(item.name())) {
                continue;
            }
            Project project = projectRepository.findFirstByNameContainingIgnoreCase(item.name().trim())
                    .orElseGet(Project::new);
            project.setName(project.getName() != null ? project.getName() : cut(item.name(), SHORT_MAX));
            project.setDuration(firstNonBlank(cut(item.period(), SHORT_MAX), project.getDuration()));
            project.setShortDescription(firstNonBlank(cut(item.description(), 500), project.getShortDescription()));
            project.setTechStack(firstNonBlank(cut(item.techStack(), 500), project.getTechStack()));
            projectRepository.save(project);
        }
    }

    private void updateLanguages(List<String> languages) {
        List<String> cleaned = languages.stream().filter(l -> !isBlank(l)).map(String::trim).toList();
        if (cleaned.isEmpty()) {
            return;
        }
        Optional<Personal> personal = personalRepository.findAll().stream().findFirst();
        personal.ifPresent(p -> {
            p.setLanguages(cut(String.join(", ", cleaned), SHORT_MAX));
            personalRepository.save(p);
        });
    }

    private static Optional<Experience> matchingExperience(List<Experience> existing, ResumeData.Role role) {
        return existing.stream()
                .filter(e -> sameText(e.getCompany(), role.company()) && sameText(e.getPosition(), role.title()))
                .findFirst()
                .or(() -> existing.stream().filter(e -> sameText(e.getCompany(), role.company())).findFirst());
    }

    /** Points trimmed, with any leading bullet character the document text carried removed. */
    static List<String> cleanBullets(List<String> bullets) {
        if (bullets == null) {
            return List.of();
        }
        return bullets.stream()
                .filter(b -> b != null)
                .map(b -> b.trim().replaceFirst("^[\\u2022\\u25CF\\u25AA*\\-]+\\s*", "").trim())
                .filter(b -> !b.isEmpty())
                .toList();
    }

    private static String cut(String value, int max) {
        if (isBlank(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    /** Shortens to {@code max} characters, dropping a trailing partial line so no bullet is cut mid-sentence. */
    private static String cutAtLine(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        int lastBreak = value.lastIndexOf('\n', max);
        return value.substring(0, lastBreak > 0 ? lastBreak : max);
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return isBlank(preferred) ? fallback : preferred;
    }

    private static boolean sameText(String a, String b) {
        return !isBlank(a) && !isBlank(b) && a.trim().toLowerCase(Locale.ROOT).equals(b.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean present(List<?> list) {
        return list != null && !list.isEmpty();
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
