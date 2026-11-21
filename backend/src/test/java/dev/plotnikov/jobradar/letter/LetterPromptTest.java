package dev.plotnikov.jobradar.letter;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plotnikov.jobradar.vacancy.Vacancy;
import org.junit.jupiter.api.Test;

class LetterPromptTest {

    private static Vacancy vacancy() {
        Vacancy v = new Vacancy();
        v.setTitle("Werkstudent Java");
        v.setCompany("Acme");
        v.setCity("Wien");
        v.setCountry("AT");
        v.setDescription("Spring Boot, PostgreSQL, REST");
        return v;
    }

    @Test
    void promptContainsVacancyAndMatchAnalysis() {
        Vacancy v = vacancy();
        v.setMatchScore(84);
        v.setMatchSummary("Strong Java fit");
        v.setMatchStrengths("Spring Boot projects\nPostgreSQL");
        v.setMatchGaps("Kubernetes");
        v.setMatchAdvice("Mention FinTrack");

        String prompt = LetterService.userPrompt(v);

        assertThat(prompt).contains("<vacancy>", "Werkstudent Java", "Company: Acme", "Wien, AT");
        assertThat(prompt).contains("<match_analysis>", "Score: 84/100", "- Spring Boot projects", "- PostgreSQL",
                "Gaps:\n- Kubernetes", "Advice: Mention FinTrack");
    }

    @Test
    void promptWithoutScoringHasNoAnalysis() {
        assertThat(LetterService.userPrompt(vacancy())).doesNotContain("match_analysis");
    }

    @Test
    void longDashesBecomeHyphens() {
        assertThat(LetterService.clean("  Java — Spring–Boot—REST \n")).isEqualTo("Java - Spring-Boot-REST");
    }

    @Test
    void templateLettersLinkThePortfolio() {
        assertThat(LetterService.templateLetter(vacancy(), "EN"))
                .startsWith("Dear Hiring Team,\n\nFor the \"Werkstudent Java\" position at Acme")
                .contains("https://boris-plotnikov.click")
                .doesNotContain("—");
        assertThat(LetterService.templateLetter(vacancy(), "DE"))
                .contains("„Werkstudent Java“ bei Acme", "https://boris-plotnikov.click", "Mit freundlichen Grüßen");
    }
}
