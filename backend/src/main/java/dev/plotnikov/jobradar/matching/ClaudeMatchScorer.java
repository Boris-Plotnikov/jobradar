package dev.plotnikov.jobradar.matching;

import dev.plotnikov.jobradar.config.JobRadarProperties;
import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import org.springframework.stereotype.Component;

@Component
public class ClaudeMatchScorer implements MatchScorer {
    static final int MAX_DESCRIPTION_FOR_AI = 4000;

    private final ClaudeClient claude;
    private final String model;

    public ClaudeMatchScorer(ClaudeClient claude, JobRadarProperties properties) {
        this.claude = claude;
        this.model = properties.ai().matchModel();
    }

    @Override
    public String method() {
        return "AI";
    }

    @Override
    public MatchResult score(Vacancy vacancy, Settings settings) {
        MatchResult result = claude.structured(model, systemPrompt(settings), userPrompt(vacancy), MatchResult.class,
                "MATCH", vacancy.getId());
        return result.validated();
    }

    public static final String INSTRUCTIONS = """
                You are a career advisor for junior software developers in Europe.
                Compare one vacancy with the candidate CV and rate the fit from 0 to 100.

                How to score:
                - Look at what the job really needs: seniority, core technologies, languages, location and hours.
                - The candidate is a student who can work up to 20 hours per week. Full-time-only roles lose points,
                  working student / part-time / junior roles gain points.
                - Missing "nice to have" skills cost little; missing core requirements cost a lot.
                - Be realistic, not polite: a senior role is a poor fit even if the technologies match.
                - Write strengths and gaps as short concrete phrases, in English.
                """;

    static String systemPrompt(Settings settings) {
        return INSTRUCTIONS + "\nCandidate CV:\n" + settings.getResumeText();
    }

    public static String userPrompt(Vacancy v) {
        String description = v.getDescription();
        if (description.length() > MAX_DESCRIPTION_FOR_AI) {
            description = description.substring(0, MAX_DESCRIPTION_FOR_AI);
        }
        return "Vacancy: " + v.getTitle() + "\n"
                + "Company: " + v.getCompany() + "\n"
                + "Location: " + (v.getCity() == null ? "" : v.getCity() + ", ") + v.getCountry()
                + " (" + v.getWorkMode() + ")\n"
                + "Language of the ad: " + v.getLanguage() + "\n"
                + "Technologies found: " + v.getSkills() + "\n\n"
                + description;
    }
}
