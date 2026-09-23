package com.askpranav.config;

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
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeds Pranav's real bio/career data, sourced from his resume
 * ({@code src/main/resources/knowledge/Lebenslauf-Pranav-Lead-Java-Dev.docx}) rather than the
 * original Biography-service SQL inserts, which had gone stale (single job, no publications).
 * Idempotent: skips a table that's already populated, so re-running the app doesn't duplicate rows.
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
    private final PublicationRepository publicationRepository;

    public DataSeeder(PersonalRepository personalRepository, EducationRepository educationRepository,
                       ExperienceRepository experienceRepository, SkillRepository skillRepository,
                       CertificationRepository certificationRepository, SummaryRepository summaryRepository,
                       ProjectRepository projectRepository, PublicationRepository publicationRepository) {
        this.personalRepository = personalRepository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.skillRepository = skillRepository;
        this.certificationRepository = certificationRepository;
        this.summaryRepository = summaryRepository;
        this.projectRepository = projectRepository;
        this.publicationRepository = publicationRepository;
    }

    @Override
    public void run(String... args) {
        seedPersonal();
        seedEducation();
        seedExperience();
        seedSkills();
        seedCertifications();
        seedSummary();
        seedProjects();
        seedPublications();
    }

    private void seedPersonal() {
        if (personalRepository.count() > 0) return;
        Personal personal = new Personal();
        // Date of birth and email are deliberately NOT seeded: this repo is public. Add them locally
        // through the Personal CRUD endpoint if you want getContactInfo() to return them.
        personal.setDob(null);
        personal.setHobbies("Competitive Swimming");
        // English/German proficiency levels are from the resume's LANGUAGES section; Hindi/Marathi
        // are native-language facts the resume (professional-facing) doesn't list but which still hold.
        personal.setLanguages("English (C1), German (A2), Hindi, Marathi");
        // TODO: fill in a portfolio URL before relying on getContactInfo(), if you want one
        personal.setEmail(null);
        personal.setLinkedinUrl("https://www.linkedin.com/in/pranav-mundhe-131059104/");
        personal.setGithubUrl("https://github.com/Pranavmundhe3");
        personal.setPortfolioUrl(null);
        personalRepository.save(personal);
        log.info("Seeded Personal");
    }

    private void seedEducation() {
        if (educationRepository.count() > 0) return;
        Education education = new Education();
        education.setDegree("Bachelor of Engineering (B.E.), Information Technology");
        education.setUniversity("University of Mumbai, India");
        education.setYearOfCompletion("2016 - 2020");
        // Grade isn't listed on the current resume; carried over from the prior seed data, unconfirmed.
        education.setGrades("8.69 / 10");
        educationRepository.save(education);
        log.info("Seeded Education");
    }

    private void seedExperience() {
        if (experienceRepository.count() > 0) return;

        Experience tSystems = new Experience();
        tSystems.setPosition("Senior Software Engineer");
        tSystems.setCompany("T-Systems, A Deutsche Telekom Company");
        tSystems.setClient("Volkswagen");
        tSystems.setLocation("India");
        tSystems.setYear("Aug 2025 - Present");
        tSystems.setDescription(
                "Modernized Volkswagen's 17-year-old Dealer workshop system integrations by replacing "
                        + "synchronous SOAP with Java, Spring Boot and event driven architecture using RabbitMQ "
                        + "messaging, extending the system's operational lifespan without a full rewrite. "
                        + "Reduced environment validation effort by 90% by designing Resilience4j fault-tolerance "
                        + "patterns and centralized monitoring dashboard for 5 environments and 25 distributed "
                        + "microservices, saving more than 5 hours/week and enabling early fault detection. "
                        + "Recognized with T-Systems India's SPOTLIGHT Award (Q1 2026) for delivering system "
                        + "transition and key module rollout. Led end-to-end delivery of more than 6 key features. "
                        + "Owned design, testing, deployment, and production support with cross functional teams "
                        + "in Germany, driving technical decisions throughout.");
        experienceRepository.save(tSystems);

        Experience here = new Experience();
        here.setPosition("Software Engineer II");
        here.setCompany("HERE Technologies");
        here.setClient(null);
        here.setLocation("India");
        here.setYear("Oct 2023 - Aug 2025");
        here.setDescription(
                "Built a real-time streaming pipeline (Java 11, Apache Camel, RabbitMQ) that ingested a new "
                        + "live accident-feed from a provider across 8 new EU countries, expanding HERE Maps' "
                        + "incident coverage in Europe and onboarding new enterprise clients. Developed AWS Lambda "
                        + "integration for 6 accident-feed providers total, eliminating manual IP-whitelisting for "
                        + "third-party communication and removing a recurring deployment downtime of 15 minutes "
                        + "every month. Embedded automated smoke tests into GitLab CI/CD pipelines to catch bugs "
                        + "and faults before release, and configured Splunk logging with new dashboards across "
                        + "distributed systems, cutting production troubleshooting time from hours to minutes.");
        experienceRepository.save(here);

        Experience ltiMindtree = new Experience();
        ltiMindtree.setPosition("Software Engineer I");
        ltiMindtree.setCompany("LTIMindtree");
        ltiMindtree.setClient("Nets DK");
        ltiMindtree.setLocation("Mumbai, India");
        ltiMindtree.setYear("Aug 2020 - Oct 2023");
        ltiMindtree.setDescription(
                "Developed high volume Java, Spring Boot microservices with Docker and Amazon EKS, supporting "
                        + "real-time transaction insights for more than 200,000 Nordic merchants. Reduced API and "
                        + "database call volume by 60% and payload size by 20% through DB query optimization, "
                        + "cutting merchant dashboard load time from 40 seconds to 5 seconds. Built dynamic "
                        + "Angular reactive forms that enabled onboarding of new POS machine types. Improved "
                        + "CI/CD pipeline execution speed by 30% by restructuring and downsizing a bloated Git "
                        + "repository, accelerating build and deployment workflows.");
        experienceRepository.save(ltiMindtree);

        log.info("Seeded Experience (3 roles: T-Systems, HERE Technologies, LTIMindtree)");
    }

    private void seedSkills() {
        if (skillRepository.count() > 0) return;
        skillRepository.save(new Skills(null,
                "Java 17, Java 11, Kotlin, Python, Spring Boot, Microservices, REST APIs, SOAP, JPA, Hibernate, "
                        + "Apache Camel, RabbitMQ", "Languages & Backend"));
        skillRepository.save(new Skills(null,
                "AWS (EC2, ECS, EKS, S3, Lambda, Elastic Beanstalk), Docker, Kubernetes", "Cloud & Containers"));
        skillRepository.save(new Skills(null,
                "OAuth 2.0, OpenID Connect, SSO, JWT, IAM, Access Token", "Auth & Identity"));
        skillRepository.save(new Skills(null,
                "JUnit 5, Mockito, TDD, GitLab CI/CD, Jenkins, Maven, SonarQube, Git", "Testing & DevOps"));
        skillRepository.save(new Skills(null, "Oracle SQL, MySQL, PostgreSQL, Flyway", "Databases"));
        skillRepository.save(new Skills(null, "Angular, TypeScript, HTML5, CSS3", "Frontend"));
        skillRepository.save(new Skills(null, "Agile, Scrum, SAFe, Technical Leadership", "Process & Leadership"));
        skillRepository.save(new Skills(null,
                "AI Assisted Development, Claude Code, Prompt Engineering, RAG", "AI-Assisted Development"));
        log.info("Seeded Skills (8 categories)");
    }

    private void seedCertifications() {
        if (certificationRepository.count() > 0) return;
        // Completion dates aren't listed on the current resume; carried over from the prior seed data.
        certificationRepository.save(new Certifications(null, "May 2021", "Microsoft Certified: Azure Fundamentals"));
        certificationRepository.save(new Certifications(null, "May 2022", "AWS Certified Cloud Practitioner"));
        log.info("Seeded Certifications");
    }

    private void seedSummary() {
        if (summaryRepository.count() > 0) return;
        Summary summary = new Summary();
        summary.setSummaryDetails(
                "Software Engineer with more than 6 years of experience designing and delivering backend "
                        + "applications using Java, Spring Framework, Spring Boot, Microservices, and REST APIs "
                        + "across Telecom, Automotive, Payments/Fintech, Banking and Financial, and Maps/Navigation "
                        + "domains. Experienced across the full software development lifecycle (SDLC), including "
                        + "design, development, testing, deployment, production troubleshooting, and maintenance. "
                        + "Strong background in AWS, Docker, Kubernetes, CI/CD, and Agile development. Led a "
                        + "feature end-to-end and driven technical decisions while collaborating across "
                        + "international teams. AWS Certified Cloud Practitioner; proficient in English (C1) and "
                        + "German (A2).");
        summaryRepository.save(summary);
        log.info("Seeded Summary");
    }

    private void seedProjects() {
        if (projectRepository.count() > 0) return;
        Project project = new Project();
        project.setName("AI-Powered Job Search Agent");
        project.setDuration("2026 - Present");
        project.setShortDescription(
                "Built an autonomous job-search agent in Python using Claude Code as the development agent, "
                        + "automating discovery and ranking of job postings from job boards, with a RAG-based "
                        + "pipeline scoring job-fit via prompt-engineered Claude API calls.");
        // The resume itself leaves the scraping framework and vector DB as unfilled placeholders -
        // carried over verbatim rather than guessed; fill these in (or via POST /project/save-project)
        // once decided.
        project.setTechStack("Python, Claude Code, Anthropic API, RAG, Prompt Engineering, "
                + "[web scraping framework - TBD], [vector DB - TBD]");
        project.setGithubUrl(null);
        project.setLiveUrl(null);
        project.setReadmeExcerpt(null);
        projectRepository.save(project);
        log.info("Seeded Project (AI-Powered Job Search Agent)");
    }

    private void seedPublications() {
        if (publicationRepository.count() > 0) return;
        Publication publication = new Publication();
        publication.setTitle("NetReconner");
        publication.setPublishedOn("March 2020");
        publication.setVenue("International Research Journal of Engineering and Technology (IRJET), "
                + "Volume 07, Issue 03");
        publication.setDescription(
                "Designed a secure Intrusion Detection System (IDS) to identify network attacks using regular "
                        + "expressions, deployed on AWS Elastic Beanstalk with RDS for data persistence.");
        publication.setTechnologies("Python, Regular Expressions, AWS Cloud, MySQL, Computer Networking");
        publication.setUrl(null);
        publicationRepository.save(publication);
        log.info("Seeded Publication (NetReconner)");
    }
}
