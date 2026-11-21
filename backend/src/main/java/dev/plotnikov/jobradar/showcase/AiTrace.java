package dev.plotnikov.jobradar.showcase;

import dev.plotnikov.jobradar.matching.MatchResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AiTrace(
        List<Candidate> candidates,
        Long vacancyId,
        String model,
        String instructions,
        int cvCharacters,
        String userPrompt,
        List<SchemaField> schema,
        MatchResult answer,
        int keywordScore,
        String keywordSummary,
        Call call,
        UsageStats usage) {

    public record Candidate(Long id, String title, String company, int score) {}

    public record SchemaField(String name, String type, String description) {}

    public record Call(Instant at, String model, int inputTokens, int outputTokens, int cacheReadTokens,
                       int cacheWriteTokens, BigDecimal costUsd, Integer durationMs) {}

    public record UsageStats(long scoringCalls, long avgInputTokens, long avgOutputTokens, long avgCacheReadTokens,
                             BigDecimal avgCostUsd, BigDecimal totalCostUsd, BigDecimal costWithoutCacheUsd,
                             int cacheSavingPercent, long letterCalls, BigDecimal avgLetterCostUsd,
                             BigDecimal spentThisMonthUsd, BigDecimal monthlyLimitUsd) {}
}
