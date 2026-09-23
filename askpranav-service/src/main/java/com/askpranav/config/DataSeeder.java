package com.askpranav.config;

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
import com.askpranav.repository.SkillRepository;
import com.askpranav.repository.SummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds the real bio/career data Pranav already published as SQL inserts in the source
 * Biography-service repo's ReadMe_Instructions.txt - carried over here via JPA (not raw SQL) so it's
 * portable regardless of how Hibernate names/cases the generated Postgres columns. Idempotent: skips
 * a table that's already populated, so re-running the app doesn't duplicate rows.
 *
 * The one seeded Project row is explicitly a placeholder - the source repo had no "Projects" concept,
 * so there's no real data to carry over. Replace it via POST /project/save-project or by editing this
 * seeder before treating AskPranav's project-related answers as truthfully grounded.
 *
 * Runs before {@link com.askpranav.ai.ingestion.KnowledgeBaseIngestionRunner} (@Order ensures this).
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final PersonalRepository personalRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final SummaryRepository summaryRepository;
    private final ProjectRepository projectRepository;

    public DataSeeder(PersonalRepository personalRepository, EducationRepository educationRepository,
                       ExperienceRepository experienceRepository, SkillRepository skillRepository,
                       CertificationRepository certificationRepository, SummaryRepository summaryRepository,
                       ProjectRepository projectRepository) {
        this.personalRepository = personalRepository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.skillRepository = skillRepository;
        this.certificationRepository = certificationRepository;
        this.summaryRepository = summaryRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public void run(String... args) {
        seedPersonal();
        seedEducation();
        seedExperience();
        seedSkills();
        seedCertifications();
        seedSummary();
        seedPlaceholderProject();
    }

    private void seedPersonal() {
        if (personalRepository.count() > 0) return;
        Personal personal = new Personal();
        personal.setDob(null);
        personal.setHobbies("Competitive Swimming");
        personal.setLanguages("English, Hindi, Marathi");
        // TODO: fill in real contact links before relying on getContactInfo()
        personal.setEmail(null);
        personal.setLinkedinUrl(null);
        personal.setGithubUrl("https://github.com/Pranavmundhe3");
        personal.setPortfolioUrl(null);
        personalRepository.save(personal);
        log.info("Seeded Personal");
    }

    private void seedEducation() {
        if (educationRepository.count() > 0) return;
        Education education = new Education();
        education.setDegree("Bachelor of Engineering (B.E), Information Technology");
        education.setUniversity("Mumbai University");
        education.setYearOfCompletion("2020");
        education.setGrades("8.69 / 10");
        educationRepository.save(education);
        log.info("Seeded Education");
    }

    private void seedExperience() {
        if (experienceRepository.count() > 0) return;
        Experience experience = new Experience();
        experience.setPosition("Software Developer");
        experience.setLocation("Mumbai, India");
        experience.setCompany("LTIMindtree");
        experience.setClient("Nets DK");
        experience.setYear("Aug 2020 - Present");
        experience.setDescription("Java Full stack development of payment gateway solutions for merchants "
                + "based in Nordic region. Development of web portal for configuration of payment gateways "
                + "which are active at merchant sales locations. Designing and developing end to end APIs, "
                + "building UI, creation of microservices and facilitate communication between them. "
                + "Collaborate with Product owners to understand new requirements and design, develop "
                + "features. Involvement in feature releases to production environment, hotfixes, "
                + "deployments, agile ceremonies. Took a part in code reviews to suggest efficient coding "
                + "practices as suggested by SonarQ. Working in agile environment following SAFe practices, "
                + "collaboration using Miro board.");
        experienceRepository.save(experience);
        log.info("Seeded Experience");
    }

    private void seedSkills() {
        if (skillRepository.count() > 0) return;
        skillRepository.save(new Skills(null,
                "JAVA 11, OOPS, JPA/Hibernate, RESTful web services, Microservices, Oracle SQL", "Technologies"));
        skillRepository.save(new Skills(null, "Angular 8, Spring boot", "Frameworks"));
        skillRepository.save(new Skills(null,
                "Git/BitBucket, Jenkins, SonarQ, OpenShift, Splunk, AWS, Maven, Flyway, RabbitMQ, Swagger, "
                        + "Postman, Mockito & Junit5", "Tools"));
        skillRepository.save(new Skills(null, "Scaled Agile (SAFe), Jira, Confluence, Miro board", "Others"));
        log.info("Seeded Skills");
    }

    private void seedCertifications() {
        if (certificationRepository.count() > 0) return;
        certificationRepository.save(new Certifications(null, "May 2021", "Microsoft Azure Cloud Fundamentals AZ-900"));
        certificationRepository.save(new Certifications(null, "May 2022", "AWS Certified Cloud Practitioner"));
        log.info("Seeded Certifications");
    }

    private void seedSummary() {
        if (summaryRepository.count() > 0) return;
        Summary summary = new Summary();
        summary.setSummaryDetails("A detail-oriented Software Engineer having strong experience in building "
                + "financial technology solutions. During my journey of Java Full Stack Development, I have "
                + "collaborated with global teams following scaled agile practices to develop rich "
                + "applications using latest technology stack. I am dedicated to perfecting my skills by "
                + "learning from more experienced developers, remaining humble and learning all that I can "
                + "in this endless journey.");
        summaryRepository.save(summary);
        log.info("Seeded Summary");
    }

    private void seedPlaceholderProject() {
        if (projectRepository.count() > 0) return;
        Project project = new Project();
        project.setName("TODO: replace with a real project");
        project.setShortDescription("TODO - this is placeholder data. Add your real projects via "
                + "POST /project/save-project so getProjectDetails and matchJobDescription are grounded "
                + "in truth rather than a placeholder.");
        project.setTechStack("TODO");
        project.setGithubUrl(null);
        project.setLiveUrl(null);
        project.setReadmeExcerpt(null);
        projectRepository.save(project);
        log.info("Seeded placeholder Project - replace with real project data");
    }
}
