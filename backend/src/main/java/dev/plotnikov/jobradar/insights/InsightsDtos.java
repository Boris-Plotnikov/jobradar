package dev.plotnikov.jobradar.insights;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class InsightsDtos {
    private InsightsDtos() {}

    public record NamedCount(String name, long count, double share) {}

    public record SalaryRow(String country, long vacancies, long avgMin, long avgMax) {}

    public record WeekRow(String week, long fetched, long relevant, Double avgScore) {}

    public record SkillGap(String skill, long demand, boolean inCv) {}

    public record Insights(
            int days,
            long totalVacancies,
            long relevantVacancies,
            Double avgScore,
            List<NamedCount> topSkills,
            List<NamedCount> topSkillsRelevant,
            List<SkillGap> cvCoverage,
            List<NamedCount> countries,
            List<NamedCount> workModes,
            List<NamedCount> languages,
            List<NamedCount> directions,
            List<SalaryRow> salaries,
            List<WeekRow> weekly,
            List<Map<String, Object>> skillTrends) {}

    public record StatsWeek(String week, long found, long applied, long interviews) {}

    public record Stats(
            List<StatsWeek> weekly,
            Map<String, Long> funnel,
            BigDecimal aiSpentThisMonth,
            BigDecimal aiMonthlyBudget,
            long aiCallsThisMonth,
            String aiOffReason,
            List<Map<String, Object>> runs) {}

    public record Summary(
            long processedLast30Days,
            long relevantLast30Days,
            long strongMatchesLast30Days,
            List<String> sources,
            String lastRunAt,
            long totalVacancies) {}
}
