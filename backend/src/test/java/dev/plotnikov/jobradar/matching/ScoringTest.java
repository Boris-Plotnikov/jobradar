package dev.plotnikov.jobradar.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plotnikov.jobradar.vacancy.SkillExtractor;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.WorkMode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScoringTest {
    private final KeywordMatchScorer keywords = new KeywordMatchScorer(new SkillExtractor());

    @Test
    void keywordScoreRewardsMatchingSkillsAndJuniorTitle() {
        Vacancy good = PrefilterTest.vacancy("Junior Java Developer", "AT", WorkMode.ONSITE, "");
        good.setSkills("Java,Spring,SQL");
        Vacancy weak = PrefilterTest.vacancy("Developer", "AT", WorkMode.ONSITE, "");
        weak.setSkills("Python,Kubernetes,AWS");

        MatchResult goodResult = keywords.score(good, PrefilterTest.settings());
        MatchResult weakResult = keywords.score(weak, PrefilterTest.settings());

        assertThat(goodResult.score()).isGreaterThanOrEqualTo(80);
        assertThat(weakResult.score()).isLessThan(30);
        assertThat(weakResult.gaps().get(0)).contains("Python");
    }

    @Test
    void aiAnswerIsValidated() {
        assertThatThrownBy(() -> new MatchResult(150, "x", List.of(), List.of(), "").validated())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchResult(50, " ", List.of(), List.of(), "").validated())
                .isInstanceOf(IllegalArgumentException.class);
        MatchResult cleaned = new MatchResult(70, " ok ", null, null, null).validated();
        assertThat(cleaned.summary()).isEqualTo("ok");
        assertThat(cleaned.strengths()).isEmpty();
    }

    @Test
    void costCalculation() {
        assertThat(AiPricing.cost("claude-haiku-5-5", 1_000_000, 1_000_000, 0, 0))
                .isEqualByComparingTo(new BigDecimal("0.60"));
        assertThat(AiPricing.cost("claude-sonnet-5-5", 0, 0, 1_000_000, 0))
                .isEqualByComparingTo(new BigDecimal("0.20"));
    }

    @Test
    void promptContainsCvAndVacancy() {
        Vacancy v = PrefilterTest.vacancy("Junior Java Developer", "AT", WorkMode.ONSITE, "Long text");
        assertThat(ClaudeMatchScorer.systemPrompt(PrefilterTest.settings())).contains("Spring Boot");
        assertThat(ClaudeMatchScorer.userPrompt(v)).contains("Junior Java Developer").contains("Long text");
    }
}
