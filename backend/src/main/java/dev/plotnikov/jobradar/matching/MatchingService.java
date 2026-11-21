package dev.plotnikov.jobradar.matching;

import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {
    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final ClaudeMatchScorer aiScorer;
    private final KeywordMatchScorer keywordScorer;
    private final AiBudget budget;
    private final Clock clock;

    public MatchingService(ClaudeMatchScorer aiScorer, KeywordMatchScorer keywordScorer, AiBudget budget, Clock clock) {
        this.aiScorer = aiScorer;
        this.keywordScorer = keywordScorer;
        this.budget = budget;
        this.clock = clock;
    }

    public String scoreAndApply(Vacancy vacancy, Settings settings, boolean allowAi) {
        MatchScorer scorer = allowAi && budget.aiAvailable() ? aiScorer : keywordScorer;
        MatchResult result;
        try {
            result = scorer.score(vacancy, settings);
        } catch (RuntimeException e) {
            log.warn("AI scoring failed for vacancy {}: {} - using keywords", vacancy.getId(), e.getMessage());
            scorer = keywordScorer;
            result = keywordScorer.score(vacancy, settings);
        }
        apply(vacancy, result, scorer.method());
        return scorer.method();
    }

    private void apply(Vacancy v, MatchResult r, String method) {
        v.setMatchScore(r.score());
        v.setMatchSummary(r.summary().length() > 1000 ? r.summary().substring(0, 1000) : r.summary());
        v.setMatchStrengths(String.join("\n", r.strengths()));
        v.setMatchGaps(String.join("\n", r.gaps()));
        v.setMatchAdvice(r.advice());
        v.setScoredBy(method);
        v.setScoredAt(Instant.now(clock));
    }
}
