package dev.plotnikov.jobradar.matching;

import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.vacancy.SkillExtractor;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class KeywordMatchScorer implements MatchScorer {
    static final Set<String> NON_TECHNICAL = Set.of("German", "Agile/Scrum");

    private final SkillExtractor skillExtractor;

    public KeywordMatchScorer(SkillExtractor skillExtractor) {
        this.skillExtractor = skillExtractor;
    }

    @Override
    public String method() {
        return "KEYWORDS";
    }

    @Override
    public MatchResult score(Vacancy vacancy, Settings settings) {
        List<String> cvSkills = skillExtractor.extract(settings.getResumeText());
        List<String> jobSkills = vacancy.skillList().stream().filter(s -> !NON_TECHNICAL.contains(s)).toList();

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : jobSkills) {
            if (cvSkills.contains(skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        int score = jobSkills.isEmpty() ? 10 : (int) Math.round(60.0 * matched.size() / jobSkills.size());

        String title = vacancy.getTitle().toLowerCase(Locale.ROOT);
        if (title.matches(".*\\b(junior|werkstudent|working student|trainee|intern|internship|praktikum|einsteiger|early careers?|graduate|entry level|berufseinsteiger|absolvent)\\b.*")) {
            score += 25;
        }
        if (title.matches(".*\\b(java|spring|backend|c\\+\\+)(?![a-z]).*")) {
            score += 10;
        }
        if ("DE".equals(vacancy.getLanguage()) && !jobSkills.isEmpty()) {
            score += 5;
        }
        score = Math.max(0, Math.min(100, score));

        List<String> strengths = matched.isEmpty() ? List.of("General software development background")
                : List.of("Matching skills: " + String.join(", ", matched));
        List<String> gaps = missing.isEmpty() ? List.of() : List.of("Not in CV: " + String.join(", ", missing));
        String summary = "Keyword estimate: " + matched.size() + " of " + jobSkills.size()
                + " technologies from the vacancy are in the CV.";
        return new MatchResult(score, summary, strengths, gaps,
                "Estimated without AI. Read the requirements before applying.").validated();
    }
}
