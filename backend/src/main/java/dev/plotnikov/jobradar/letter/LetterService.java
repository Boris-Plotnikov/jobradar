package dev.plotnikov.jobradar.letter;

import dev.plotnikov.jobradar.common.NotFoundException;
import dev.plotnikov.jobradar.config.JobRadarProperties;
import dev.plotnikov.jobradar.matching.AiBudget;
import dev.plotnikov.jobradar.matching.ClaudeClient;
import dev.plotnikov.jobradar.settings.SettingsService;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.VacancyRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LetterService {
    private static final Logger log = LoggerFactory.getLogger(LetterService.class);
    static final Set<String> LANGUAGES = Set.of("EN", "DE");

    private final CoverLetterRepository letters;
    private final PromptTemplateRepository templates;
    private final VacancyRepository vacancies;
    private final SettingsService settings;
    private final ClaudeClient claude;
    private final AiBudget budget;
    private final JobRadarProperties properties;
    private final Clock clock;

    public LetterService(CoverLetterRepository letters, PromptTemplateRepository templates,
                         VacancyRepository vacancies, SettingsService settings, ClaudeClient claude, AiBudget budget,
                         JobRadarProperties properties, Clock clock) {
        this.letters = letters;
        this.templates = templates;
        this.vacancies = vacancies;
        this.settings = settings;
        this.claude = claude;
        this.budget = budget;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CoverLetter> versions(Long vacancyId) {
        return letters.findByVacancyIdOrderByVersionDesc(vacancyId);
    }

    @Transactional
    public CoverLetter generate(Long vacancyId, String language) {
        String lang = checkLanguage(language);
        Vacancy vacancy = vacancies.findById(vacancyId)
                .orElseThrow(() -> new NotFoundException("Vacancy " + vacancyId + " not found"));
        String resume = settings.get().getResumeText();

        String body;
        String generatedBy;
        if (budget.aiAvailable()) {
            try {
                PromptTemplate template = templates.findByCode("COVER_LETTER_" + lang)
                        .orElseThrow(() -> new IllegalStateException("Template COVER_LETTER_" + lang + " missing"));
                String system = template.getBody() + "\n\nReturn only the letter text, without a subject line.\n\n"
                        + "Candidate CV:\n" + resume;
                body = claude.text(properties.ai().letterModel(), system, vacancyText(vacancy), "LETTER", vacancyId);
                generatedBy = "AI";
            } catch (RuntimeException e) {
                log.warn("AI letter failed: {} - using template", e.getMessage());
                body = templateLetter(vacancy, lang);
                generatedBy = "TEMPLATE";
            }
        } else {
            body = templateLetter(vacancy, lang);
            generatedBy = "TEMPLATE";
        }
        return saveVersion(vacancyId, lang, body, generatedBy);
    }

    @Transactional
    public CoverLetter saveManual(Long vacancyId, String language, String body) {
        if (!vacancies.existsById(vacancyId)) {
            throw new NotFoundException("Vacancy " + vacancyId + " not found");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Letter text is empty");
        }
        return saveVersion(vacancyId, checkLanguage(language), body.trim(), "MANUAL");
    }

    @Transactional(readOnly = true)
    public List<PromptTemplate> templates() {
        return templates.findAll();
    }

    @Transactional
    public PromptTemplate updateTemplate(Long id, String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("Template text is empty");
        }
        PromptTemplate t = templates.findById(id).orElseThrow(() -> new NotFoundException("Template not found"));
        t.setBody(body.trim());
        t.setUpdatedAt(Instant.now(clock));
        return t;
    }

    private CoverLetter saveVersion(Long vacancyId, String language, String body, String generatedBy) {
        int next = letters.findFirstByVacancyIdAndLanguageOrderByVersionDesc(vacancyId, language)
                .map(l -> l.getVersion() + 1)
                .orElse(1);
        CoverLetter letter = new CoverLetter();
        letter.setVacancyId(vacancyId);
        letter.setLanguage(language);
        letter.setVersion(next);
        letter.setBody(body);
        letter.setGeneratedBy(generatedBy);
        letter.setCreatedAt(Instant.now(clock));
        return letters.save(letter);
    }

    static String checkLanguage(String language) {
        String lang = language == null ? "" : language.toUpperCase();
        if (!LANGUAGES.contains(lang)) {
            throw new IllegalArgumentException("Language must be EN or DE");
        }
        return lang;
    }

    static String vacancyText(Vacancy v) {
        String description = v.getDescription();
        if (description.length() > 5000) {
            description = description.substring(0, 5000);
        }
        return "Vacancy: " + v.getTitle() + "\nCompany: " + v.getCompany() + "\nLocation: "
                + (v.getCity() == null ? "" : v.getCity() + ", ") + v.getCountry() + "\n\n" + description;
    }

    static String templateLetter(Vacancy v, String language) {
        if ("DE".equals(language)) {
            return """
                    Sehr geehrte Damen und Herren,

                    mit großem Interesse habe ich Ihre Ausschreibung „%s“ bei %s gelesen. Als Student der Informatik \
                    an der Universität Wien entwickle ich Backend-Anwendungen mit Java und Spring Boot und habe \
                    mehrere Projekte mit PostgreSQL, REST-APIs und automatisierten Tests umgesetzt.

                    Vor dem Studium habe ich sechs Jahre lang Einkauf und IT in einem Unternehmen geleitet. Dadurch \
                    verstehe ich, wie Software echte Geschäftsprozesse unterstützt, und arbeite strukturiert und \
                    verlässlich. Ich spreche Deutsch (C1), Englisch (C2) und Russisch.

                    Neben dem Studium kann ich bis zu 20 Stunden pro Woche arbeiten. Über die Gelegenheit zu einem \
                    Gespräch freue ich mich sehr.

                    Mit freundlichen Grüßen
                    Boris Plotnikov""".formatted(v.getTitle(), v.getCompany());
        }
        return """
                Dear Hiring Team,

                I was glad to see the "%s" opening at %s. I study Computer Science at the University of Vienna and \
                build backend applications with Java and Spring Boot; my portfolio includes several projects with \
                PostgreSQL, REST APIs and automated tests.

                Before university I led procurement and IT at a company for six years. This taught me how software \
                supports real business processes and how to work in a structured, reliable way. I speak English \
                (C2), German (C1) and Russian.

                I can work up to 20 hours per week alongside my studies. I would be happy to talk about how I can \
                help your team.

                Kind regards,
                Boris Plotnikov""".formatted(v.getTitle(), v.getCompany());
    }
}
