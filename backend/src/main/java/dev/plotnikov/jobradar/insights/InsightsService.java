package dev.plotnikov.jobradar.insights;

import dev.plotnikov.jobradar.application.ApplicationRepository;
import dev.plotnikov.jobradar.application.ApplicationStatus;
import dev.plotnikov.jobradar.application.JobApplication;
import dev.plotnikov.jobradar.config.JobRadarProperties;
import dev.plotnikov.jobradar.insights.InsightsDtos.Insights;
import dev.plotnikov.jobradar.insights.InsightsDtos.NamedCount;
import dev.plotnikov.jobradar.insights.InsightsDtos.SalaryRow;
import dev.plotnikov.jobradar.insights.InsightsDtos.SkillGap;
import dev.plotnikov.jobradar.insights.InsightsDtos.Stats;
import dev.plotnikov.jobradar.insights.InsightsDtos.StatsWeek;
import dev.plotnikov.jobradar.insights.InsightsDtos.Summary;
import dev.plotnikov.jobradar.insights.InsightsDtos.WeekRow;
import dev.plotnikov.jobradar.matching.AiBudget;
import dev.plotnikov.jobradar.matching.AiUsageRepository;
import dev.plotnikov.jobradar.pipeline.PipelineRun;
import dev.plotnikov.jobradar.pipeline.PipelineRunRepository;
import dev.plotnikov.jobradar.settings.SettingsService;
import dev.plotnikov.jobradar.vacancy.SkillExtractor;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.VacancyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsightsService {
    private final VacancyRepository vacancies;
    private final ApplicationRepository applications;
    private final PipelineRunRepository runs;
    private final AiBudget budget;
    private final AiUsageRepository aiUsage;
    private final SettingsService settings;
    private final SkillExtractor skillExtractor;
    private final ZoneId zone;
    private final Clock clock;

    public InsightsService(VacancyRepository vacancies, ApplicationRepository applications, PipelineRunRepository runs,
                           AiBudget budget, AiUsageRepository aiUsage, SettingsService settings,
                           SkillExtractor skillExtractor, JobRadarProperties properties, Clock clock) {
        this.vacancies = vacancies;
        this.applications = applications;
        this.runs = runs;
        this.budget = budget;
        this.aiUsage = aiUsage;
        this.settings = settings;
        this.skillExtractor = skillExtractor;
        this.zone = ZoneId.of(properties.timezone());
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Insights insights(int days) {
        int period = Math.max(1, Math.min(days, 365));
        List<Vacancy> all = vacancies.findByFetchedAtAfter(Instant.now(clock).minus(Duration.ofDays(period)));
        List<Vacancy> relevant = all.stream().filter(Vacancy::isPrefilterPassed).toList();

        List<NamedCount> topSkillsRelevant = countMany(relevant, Vacancy::skillList, 15);
        List<String> cvSkills = skillExtractor.extract(settings.get().getResumeText());
        List<SkillGap> coverage = topSkillsRelevant.stream()
                .map(s -> new SkillGap(s.name(), s.count(), cvSkills.contains(s.name())))
                .toList();

        return new Insights(
                period,
                all.size(),
                relevant.size(),
                average(relevant.stream().map(Vacancy::getMatchScore).filter(Objects::nonNull).toList()),
                countMany(all, Vacancy::skillList, 15),
                topSkillsRelevant,
                coverage,
                countOne(all, v -> v.getCountry() == null ? "Unknown" : v.getCountry(), 12),
                countOne(all, v -> v.getWorkMode().name(), 3),
                countOne(all, Vacancy::getLanguage, 3),
                countOne(relevant, v -> v.getDirection() == null ? "Other" : v.getDirection(), 10),
                salaries(all),
                weekly(all),
                skillTrends(relevant, topSkillsRelevant.stream().limit(5).map(NamedCount::name).toList()));
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        Instant since = Instant.now(clock).minus(Duration.ofDays(84));
        Map<String, long[]> weeks = new TreeMap<>();
        for (Vacancy v : vacancies.findByFetchedAtAfter(since)) {
            if (v.isPrefilterPassed()) {
                weeks.computeIfAbsent(week(v.getFetchedAt()), k -> new long[3])[0]++;
            }
        }
        for (JobApplication a : applications.findAll()) {
            if (a.getAppliedAt() != null && a.getAppliedAt().isAfter(since)) {
                weeks.computeIfAbsent(week(a.getAppliedAt()), k -> new long[3])[1]++;
            }
            boolean interview = a.getStatus() == ApplicationStatus.INTERVIEW || a.getStatus() == ApplicationStatus.OFFER;
            if (interview && a.getStatusChangedAt().isAfter(since)) {
                weeks.computeIfAbsent(week(a.getStatusChangedAt()), k -> new long[3])[2]++;
            }
        }
        List<StatsWeek> weekly = weeks.entrySet().stream()
                .map(e -> new StatsWeek(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();

        Map<String, Long> funnel = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            funnel.put(status.name(), applications.countByStatus(status));
        }

        List<Map<String, Object>> recentRuns = new ArrayList<>();
        for (PipelineRun r : runs.findTop20ByOrderByStartedAtDesc()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("startedAt", r.getStartedAt());
            row.put("trigger", r.getTriggerType());
            row.put("status", r.getStatus());
            row.put("fetched", r.getFetched());
            row.put("new", r.getNewVacancies());
            row.put("duplicates", r.getDuplicates());
            row.put("passed", r.getPassedPrefilter());
            row.put("scored", r.getScored());
            row.put("log", r.getLog());
            recentRuns.add(row);
        }

        Instant monthStart = LocalDate.now(clock.withZone(zone)).withDayOfMonth(1).atStartOfDay(zone).toInstant();
        return new Stats(weekly, funnel, budget.spentThisMonth(), budget.limit(),
                aiUsage.countByCreatedAtAfter(monthStart), budget.offReason(), recentRuns);
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        Instant since = Instant.now(clock).minus(Duration.ofDays(30));
        List<Vacancy> last30 = vacancies.findByFetchedAtAfter(since);
        String lastRun = runs.findFirstByStatusOrderByStartedAtDesc("OK")
                .map(r -> r.getFinishedAt() == null ? null : r.getFinishedAt().toString())
                .orElse(null);
        return new Summary(
                last30.size(),
                last30.stream().filter(Vacancy::isPrefilterPassed).count(),
                last30.stream().filter(v -> v.getMatchScore() != null && v.getMatchScore() >= 80).count(),
                vacancies.findAllSources(),
                lastRun,
                vacancies.count());
    }

    private static List<NamedCount> countOne(List<Vacancy> list, Function<Vacancy, String> key, int limit) {
        Map<String, Long> counts = list.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
        return toNamedCounts(counts, list.size(), limit);
    }

    private static List<NamedCount> countMany(List<Vacancy> list, Function<Vacancy, List<String>> keys, int limit) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Vacancy v : list) {
            for (String k : keys.apply(v)) {
                counts.merge(k, 1L, Long::sum);
            }
        }
        return toNamedCounts(counts, list.size(), limit);
    }

    private static List<NamedCount> toNamedCounts(Map<String, Long> counts, int total, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .map(e -> new NamedCount(e.getKey(), e.getValue(),
                        total == 0 ? 0 : Math.round(1000.0 * e.getValue() / total) / 10.0))
                .toList();
    }

    private static List<SalaryRow> salaries(List<Vacancy> list) {
        Map<String, List<Vacancy>> byCountry = list.stream()
                .filter(v -> v.getSalaryMin() != null && v.getSalaryMax() != null && v.getSalaryMin() > 0)
                .filter(v -> v.getSalaryCurrency() == null || "EUR".equals(v.getSalaryCurrency()))
                .collect(Collectors.groupingBy(v -> v.getCountry() == null ? "Unknown" : v.getCountry()));
        return byCountry.entrySet().stream()
                .map(e -> new SalaryRow(e.getKey(), e.getValue().size(),
                        Math.round(e.getValue().stream().mapToInt(Vacancy::getSalaryMin).average().orElse(0)),
                        Math.round(e.getValue().stream().mapToInt(Vacancy::getSalaryMax).average().orElse(0))))
                .sorted(Comparator.comparingLong(SalaryRow::vacancies).reversed())
                .toList();
    }

    private List<WeekRow> weekly(List<Vacancy> list) {
        Map<String, List<Vacancy>> byWeek = list.stream()
                .collect(Collectors.groupingBy(v -> week(v.getFetchedAt()), TreeMap::new, Collectors.toList()));
        return byWeek.entrySet().stream()
                .map(e -> new WeekRow(e.getKey(), e.getValue().size(),
                        e.getValue().stream().filter(Vacancy::isPrefilterPassed).count(),
                        average(e.getValue().stream().map(Vacancy::getMatchScore).filter(Objects::nonNull).toList())))
                .toList();
    }

    private List<Map<String, Object>> skillTrends(List<Vacancy> relevant, List<String> skills) {
        Map<String, List<Vacancy>> byWeek = relevant.stream()
                .collect(Collectors.groupingBy(v -> week(v.getFetchedAt()), TreeMap::new, Collectors.toList()));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, List<Vacancy>> e : byWeek.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("week", e.getKey());
            for (String skill : skills) {
                row.put(skill, e.getValue().stream().filter(v -> v.skillList().contains(skill)).count());
            }
            rows.add(row);
        }
        return rows;
    }

    private String week(Instant instant) {
        LocalDate date = instant.atZone(zone).toLocalDate();
        return date.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                + String.format("%02d", date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    private static Double average(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        return Math.round(values.stream().mapToInt(Integer::intValue).average().orElse(0) * 10) / 10.0;
    }
}
