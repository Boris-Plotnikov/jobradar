package dev.plotnikov.jobradar.pipeline;

import dev.plotnikov.jobradar.config.JobRadarProperties;
import dev.plotnikov.jobradar.matching.MatchingService;
import dev.plotnikov.jobradar.matching.Prefilter;
import dev.plotnikov.jobradar.matching.PrefilterResult;
import dev.plotnikov.jobradar.notify.TelegramNotifier;
import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.settings.SettingsService;
import dev.plotnikov.jobradar.source.JobSource;
import dev.plotnikov.jobradar.source.RawJob;
import dev.plotnikov.jobradar.vacancy.Fingerprints;
import dev.plotnikov.jobradar.vacancy.Normalizer;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.VacancyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PipelineService {
    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);
    static final Duration DUPLICATE_WINDOW = Duration.ofDays(30);

    private final List<JobSource> sources;
    private final Normalizer normalizer;
    private final VacancyRepository vacancies;
    private final Prefilter prefilter;
    private final MatchingService matching;
    private final SettingsService settingsService;
    private final PipelineRunRepository runs;
    private final TelegramNotifier telegram;
    private final JobRadarProperties properties;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public PipelineService(List<JobSource> sources, Normalizer normalizer, VacancyRepository vacancies,
                           Prefilter prefilter, MatchingService matching, SettingsService settingsService,
                           PipelineRunRepository runs, TelegramNotifier telegram, JobRadarProperties properties,
                           Clock clock) {
        this.sources = sources;
        this.normalizer = normalizer;
        this.vacancies = vacancies;
        this.prefilter = prefilter;
        this.matching = matching;
        this.settingsService = settingsService;
        this.runs = runs;
        this.telegram = telegram;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean isRunning() {
        return running.get();
    }

    public PipelineRun run(String trigger) {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("The pipeline is already running");
        }
        PipelineRun run = new PipelineRun();
        run.setStartedAt(Instant.now(clock));
        run.setTriggerType(trigger);
        run.setStatus("RUNNING");
        run = runs.save(run);
        try {
            Settings settings = settingsService.get();
            List<Vacancy> fresh = fetchAndStore(run, settings);
            scorePending(run, settings);
            sendDigest(run, settings);
            run.setStatus("OK");
            log.info("Pipeline done: fetched={} new={} duplicates={} passed={} scored={}", run.getFetched(),
                    fresh.size(), run.getDuplicates(), run.getPassedPrefilter(), run.getScored());
        } catch (RuntimeException e) {
            log.error("Pipeline failed", e);
            run.setStatus("FAILED");
            run.appendLog("FAILED: " + e.getMessage());
        } finally {
            run.setFinishedAt(Instant.now(clock));
            runs.save(run);
            running.set(false);
        }
        return run;
    }

    public PipelineRun reapplyFilters() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("The pipeline is already running");
        }
        PipelineRun run = new PipelineRun();
        run.setStartedAt(Instant.now(clock));
        run.setTriggerType("REFILTER");
        run.setStatus("RUNNING");
        run = runs.save(run);
        try {
            Settings settings = settingsService.get();
            List<Vacancy> recent = vacancies.findByFetchedAtAfter(Instant.now(clock).minus(DUPLICATE_WINDOW));
            for (Vacancy v : recent) {
                PrefilterResult check = prefilter.check(v, settings);
                v.setPrefilterPassed(check.passed());
                v.setPrefilterReason(check.reason());
                v.setDirection(check.direction());
                if (!check.passed() || "KEYWORDS".equals(v.getScoredBy())) {
                    v.setMatchScore(null);
                    v.setScoredBy(null);
                }
                if (check.passed()) {
                    run.setPassedPrefilter(run.getPassedPrefilter() + 1);
                }
            }
            vacancies.saveAll(recent);
            run.setFetched(recent.size());
            run.appendLog("Re-checked " + recent.size() + " vacancies, " + run.getPassedPrefilter() + " relevant");
            scorePending(run, settings);
            run.setStatus("OK");
        } catch (RuntimeException e) {
            log.error("Re-filter failed", e);
            run.setStatus("FAILED");
            run.appendLog("FAILED: " + e.getMessage());
        } finally {
            run.setFinishedAt(Instant.now(clock));
            runs.save(run);
            running.set(false);
        }
        return run;
    }

    List<Vacancy> fetchAndStore(PipelineRun run, Settings settings) {
        Instant now = Instant.now(clock);
        List<Vacancy> stored = new ArrayList<>();
        Set<String> seenThisRun = new HashSet<>();

        for (JobSource source : sources) {
            if (!source.isEnabled()) {
                run.appendLog(source.name() + ": skipped (no API key)");
                continue;
            }
            List<RawJob> raw;
            try {
                raw = source.fetch();
            } catch (RuntimeException e) {
                run.appendLog(source.name() + ": ERROR " + e.getMessage());
                log.warn("Source {} failed: {}", source.name(), e.getMessage());
                continue;
            }
            int added = 0;
            for (RawJob job : raw) {
                run.setFetched(run.getFetched() + 1);
                if (job.externalId() == null || vacancies.existsBySourceAndExternalId(job.source(), job.externalId())) {
                    continue;
                }
                Vacancy v;
                try {
                    v = normalizer.normalize(job, now);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                if (!seenThisRun.add(v.getFingerprint()) || isDuplicate(v, now)) {
                    run.setDuplicates(run.getDuplicates() + 1);
                    continue;
                }
                PrefilterResult check = prefilter.check(v, settings);
                v.setPrefilterPassed(check.passed());
                v.setPrefilterReason(check.reason());
                v.setDirection(check.direction());
                if (check.passed()) {
                    run.setPassedPrefilter(run.getPassedPrefilter() + 1);
                }
                stored.add(vacancies.save(v));
                added++;
            }
            run.appendLog(source.name() + ": " + raw.size() + " fetched, " + added + " new");
        }
        run.setNewVacancies(stored.size());
        return stored;
    }

    boolean isDuplicate(Vacancy candidate, Instant now) {
        if (!vacancies.findByFingerprint(candidate.getFingerprint()).isEmpty()) {
            return true;
        }
        List<Vacancy> sameCompany = vacancies.findByCompanyIgnoreCaseAndFetchedAtAfter(
                candidate.getCompany(), now.minus(DUPLICATE_WINDOW));
        for (Vacancy other : sameCompany) {
            boolean sameCountry = candidate.getCountry() == null || other.getCountry() == null
                    || candidate.getCountry().equals(other.getCountry());
            if (sameCountry && Fingerprints.titleSimilarity(candidate.getTitle(), other.getTitle())
                    >= Fingerprints.SIMILAR_TITLE_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    void scorePending(PipelineRun run, Settings settings) {
        List<Vacancy> pending = vacancies.findByPrefilterPassedTrueAndMatchScoreIsNullOrderByFetchedAtDesc();
        int aiCalls = 0;
        for (Vacancy v : pending) {
            boolean allowAi = aiCalls < properties.pipeline().maxAiPerRun();
            String method = matching.scoreAndApply(v, settings, allowAi);
            if ("AI".equals(method)) {
                aiCalls++;
            }
            vacancies.save(v);
            run.setScored(run.getScored() + 1);
            if (v.getMatchScore() >= settings.getMinScore()) {
                run.setTopMatches(run.getTopMatches() + 1);
            }
        }
        run.appendLog("Scored " + pending.size() + " vacancies, " + aiCalls + " with AI");
    }

    void sendDigest(PipelineRun run, Settings settings) {
        List<Vacancy> top = vacancies.findByNotifiedFalseAndMatchScoreGreaterThanEqualOrderByMatchScoreDesc(
                settings.getMinScore());
        List<Vacancy> visible = top.stream().filter(v -> !v.isHidden()).limit(10).toList();
        if (visible.isEmpty()) {
            return;
        }
        boolean sent = telegram.sendDigest(visible, run);
        if (sent) {
            top.forEach(v -> v.setNotified(true));
            vacancies.saveAll(top);
            run.appendLog("Telegram digest: " + visible.size() + " vacancies");
        }
    }
}
