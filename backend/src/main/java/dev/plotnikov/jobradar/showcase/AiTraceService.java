package dev.plotnikov.jobradar.showcase;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import dev.plotnikov.jobradar.common.NotFoundException;
import dev.plotnikov.jobradar.config.JobRadarProperties;
import dev.plotnikov.jobradar.matching.AiBudget;
import dev.plotnikov.jobradar.matching.AiPricing;
import dev.plotnikov.jobradar.matching.AiUsage;
import dev.plotnikov.jobradar.matching.AiUsageRepository;
import dev.plotnikov.jobradar.matching.ClaudeMatchScorer;
import dev.plotnikov.jobradar.matching.KeywordMatchScorer;
import dev.plotnikov.jobradar.matching.MatchResult;
import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.settings.SettingsService;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.VacancyFilter;
import dev.plotnikov.jobradar.vacancy.VacancyRepository;
import dev.plotnikov.jobradar.vacancy.VacancyService;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiTraceService {
    private final VacancyService vacancyService;
    private final VacancyRepository vacancies;
    private final AiUsageRepository usage;
    private final SettingsService settings;
    private final KeywordMatchScorer keywordScorer;
    private final AiBudget budget;
    private final JobRadarProperties properties;
    private final Clock clock;

    public AiTraceService(VacancyService vacancyService, VacancyRepository vacancies, AiUsageRepository usage,
                          SettingsService settings, KeywordMatchScorer keywordScorer, AiBudget budget,
                          JobRadarProperties properties, Clock clock) {
        this.vacancyService = vacancyService;
        this.vacancies = vacancies;
        this.usage = usage;
        this.settings = settings;
        this.keywordScorer = keywordScorer;
        this.budget = budget;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiTrace trace(Long vacancyId) {
        List<AiTrace.Candidate> candidates = vacancyService.feed(VacancyFilter.publicShowcase(), "score", 0, 30)
                .getContent().stream()
                .filter(v -> "AI".equals(v.scoredBy()))
                .map(v -> new AiTrace.Candidate(v.id(), v.title(), v.company(), v.matchScore()))
                .toList();
        AiTrace.UsageStats stats = usageStats();
        if (candidates.isEmpty()) {
            return new AiTrace(candidates, null, properties.ai().matchModel(), ClaudeMatchScorer.INSTRUCTIONS, 0,
                    null, schema(), null, 0, null, null, stats);
        }

        Long id = vacancyId != null ? vacancyId : candidates.get(0).id();
        if (candidates.stream().noneMatch(c -> c.id().equals(id))) {
            throw new NotFoundException("Vacancy " + id + " is not on the public page");
        }
        Vacancy v = vacancies.findById(id).orElseThrow(() -> new NotFoundException("Vacancy " + id + " not found"));
        Settings s = settings.get();
        MatchResult keywords = keywordScorer.score(v, s);
        AiTrace.Call call = usage.findFirstByVacancyIdAndPurposeOrderByCreatedAtDesc(id, "MATCH")
                .map(u -> new AiTrace.Call(u.getCreatedAt(), u.getModel(), u.getInputTokens(), u.getOutputTokens(),
                        u.getCacheReadTokens(), u.getCacheWriteTokens(), u.getCostUsd(), u.getDurationMs()))
                .orElse(null);

        return new AiTrace(candidates, id, properties.ai().matchModel(), ClaudeMatchScorer.INSTRUCTIONS,
                s.getResumeText().length(), ClaudeMatchScorer.userPrompt(v), schema(), savedAnswer(v),
                keywords.score(), keywords.summary(), call, stats);
    }

    static List<AiTrace.SchemaField> schema() {
        return Arrays.stream(MatchResult.class.getRecordComponents())
                .map(c -> new AiTrace.SchemaField(c.getName(), simpleType(c), description(c)))
                .toList();
    }

    private static String simpleType(RecordComponent c) {
        return c.getGenericType().getTypeName().replaceAll("\\b[a-z]+(\\.[a-z]+)*\\.", "");
    }

    private static String description(RecordComponent c) {
        JsonPropertyDescription d = c.getAnnotation(JsonPropertyDescription.class);
        if (d == null) {
            d = c.getAccessor().getAnnotation(JsonPropertyDescription.class);
        }
        return d == null ? "" : d.value();
    }

    private static MatchResult savedAnswer(Vacancy v) {
        return new MatchResult(v.getMatchScore(), v.getMatchSummary(), Vacancy.lines(v.getMatchStrengths()),
                Vacancy.lines(v.getMatchGaps()), v.getMatchAdvice());
    }

    private AiTrace.UsageStats usageStats() {
        Instant from = Instant.now(clock).minus(Duration.ofDays(30));
        List<AiUsage> scoring = usage.findByPurposeAndCreatedAtAfter("MATCH", from);
        List<AiUsage> letters = usage.findByPurposeAndCreatedAtAfter("LETTER", from);
        return summarize(scoring, letters, budget.spentThisMonth(), budget.limit());
    }

    static AiTrace.UsageStats summarize(List<AiUsage> scoring, List<AiUsage> letters, BigDecimal spent,
                                        BigDecimal limit) {
        BigDecimal total = sumCost(scoring);

        BigDecimal withoutCache = BigDecimal.ZERO;
        for (AiUsage u : scoring) {
            long allInput = (long) u.getInputTokens() + u.getCacheReadTokens() + u.getCacheWriteTokens();
            withoutCache = withoutCache.add(AiPricing.cost(u.getModel(), allInput, u.getOutputTokens(), 0, 0));
        }
        int saving = withoutCache.signum() == 0 ? 0
                : BigDecimal.ONE.subtract(total.divide(withoutCache, 4, RoundingMode.HALF_UP))
                        .multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();

        return new AiTrace.UsageStats(
                scoring.size(),
                average(scoring.stream().mapToLong(AiUsage::getInputTokens).sum(), scoring.size()),
                average(scoring.stream().mapToLong(AiUsage::getOutputTokens).sum(), scoring.size()),
                average(scoring.stream().mapToLong(AiUsage::getCacheReadTokens).sum(), scoring.size()),
                averageCost(total, scoring.size()),
                total,
                withoutCache.setScale(6, RoundingMode.HALF_UP),
                saving,
                letters.size(),
                averageCost(sumCost(letters), letters.size()),
                spent,
                limit);
    }

    private static BigDecimal sumCost(List<AiUsage> rows) {
        return rows.stream().map(AiUsage::getCostUsd).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static long average(long sum, int count) {
        return count == 0 ? 0 : Math.round((double) sum / count);
    }

    private static BigDecimal averageCost(BigDecimal sum, int count) {
        return count == 0 ? BigDecimal.ZERO : sum.divide(BigDecimal.valueOf(count), 6, RoundingMode.HALF_UP);
    }
}
