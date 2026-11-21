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
                String system = template.getBody() + "\n\n<cv>\n" + resume + "\n</cv>";
                body = clean(claude.text(properties.ai().letterModel(), system, userPrompt(vacancy), "LETTER", vacancyId));
                if (body.isBlank()) {
                    throw new IllegalStateException("AI returned an empty letter");
                }
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

    static String userPrompt(Vacancy v) {
        StringBuilder text = new StringBuilder("<vacancy>\n").append(vacancyText(v)).append("\n</vacancy>");
        if (v.getMatchSummary() != null) {
            text.append("\n\n<match_analysis>\nScore: ").append(v.getMatchScore()).append("/100\n")
                    .append("Summary: ").append(v.getMatchSummary()).append('\n');
            appendList(text, "Strengths", v.getMatchStrengths());
            appendList(text, "Gaps", v.getMatchGaps());
            if (v.getMatchAdvice() != null && !v.getMatchAdvice().isBlank()) {
                text.append("Advice: ").append(v.getMatchAdvice()).append('\n');
            }
            text.append("</match_analysis>");
        }
        return text.toString();
    }

    private static void appendList(StringBuilder text, String title, String lines) {
        if (lines == null || lines.isBlank()) {
            return;
        }
        text.append(title).append(":\n");
        for (String line : lines.split("\n")) {
            if (!line.isBlank()) {
                text.append("- ").append(line.trim()).append('\n');
            }
        }
    }

    static String clean(String letter) {
        return letter.replace(" — ", " - ").replace("—", "-").replace("–", "-").trim();
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

                    für die Stelle „%s“ bei %s bringe ich Erfahrung mit Java, Spring Boot und PostgreSQL mit. Ich \
                    studiere Informatik an der Universität Wien und habe fünf Projekte gebaut, die online laufen, \
                    darunter eine Buchhaltung für drei Standorte und einen Purchase-to-Pay-Prozess mit \
                    Freigaben nach Betrag, beide mit REST-API, Spring Security und automatisierten Tests.

                    Vor dem Studium habe ich sechs Jahre Einkauf und IT in einem Gastransportunternehmen geleitet. \
                    Die Geschäftsprozesse hinter solcher Software kenne ich daher aus der Praxis. Ich spreche \
                    Deutsch (C1), Englisch (C2) und Russisch.

                    Neben dem Studium arbeite ich bis zu 20 Stunden pro Woche. Live-Demos und Quellcode finden Sie \
                    unter https://boris-plotnikov.click. Über ein Gespräch freue ich mich.

                    Mit freundlichen Grüßen
                    Boris Plotnikov""".formatted(v.getTitle(), v.getCompany());
        }
        return """
                Dear Hiring Team,

                For the "%s" position at %s I bring hands-on experience with Java, Spring Boot and PostgreSQL. \
                I study Computer Science at the University of Vienna and have built five projects that run \
                online, among them an accounting system for three offices and a purchase-to-pay workflow with \
                approvals by amount, both with a REST API, Spring Security and automated tests.

                Before university I led procurement and IT at a gas transportation company for six years, so I \
                know the business processes behind this kind of software from practice. I speak English (C2), \
                German (C1) and Russian.

                I can work up to 20 hours per week alongside my studies. Live demos and source code are at \
                https://boris-plotnikov.click. I would be glad to talk.

                Kind regards,
                Boris Plotnikov""".formatted(v.getTitle(), v.getCompany());
    }
}
