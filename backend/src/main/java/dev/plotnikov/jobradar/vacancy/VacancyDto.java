package dev.plotnikov.jobradar.vacancy;

import java.time.Instant;
import java.util.List;

public record VacancyDto(
        Long id,
        String source,
        String url,
        String title,
        String company,
        String country,
        String city,
        WorkMode workMode,
        String language,
        Integer salaryMin,
        Integer salaryMax,
        String salaryCurrency,
        List<String> skills,
        String direction,
        Instant publishedAt,
        Instant fetchedAt,
        boolean prefilterPassed,
        String prefilterReason,
        Integer matchScore,
        String matchSummary,
        List<String> matchStrengths,
        List<String> matchGaps,
        String matchAdvice,
        String scoredBy,
        boolean hidden,
        String description,
        String applicationStatus) {
    public static VacancyDto card(Vacancy v, String applicationStatus) {
        return new VacancyDto(v.getId(), v.getSource(), v.getUrl(), v.getTitle(), v.getCompany(), v.getCountry(),
                v.getCity(), v.getWorkMode(), v.getLanguage(), v.getSalaryMin(), v.getSalaryMax(),
                v.getSalaryCurrency(), v.skillList(), v.getDirection(), v.getPublishedAt(), v.getFetchedAt(),
                v.isPrefilterPassed(), v.getPrefilterReason(), v.getMatchScore(), v.getMatchSummary(),
                null, null, null, v.getScoredBy(), v.isHidden(), null, applicationStatus);
    }

    public static VacancyDto full(Vacancy v, String applicationStatus) {
        return new VacancyDto(v.getId(), v.getSource(), v.getUrl(), v.getTitle(), v.getCompany(), v.getCountry(),
                v.getCity(), v.getWorkMode(), v.getLanguage(), v.getSalaryMin(), v.getSalaryMax(),
                v.getSalaryCurrency(), v.skillList(), v.getDirection(), v.getPublishedAt(), v.getFetchedAt(),
                v.isPrefilterPassed(), v.getPrefilterReason(), v.getMatchScore(), v.getMatchSummary(),
                Vacancy.lines(v.getMatchStrengths()), Vacancy.lines(v.getMatchGaps()), v.getMatchAdvice(),
                v.getScoredBy(), v.isHidden(), v.getDescription(), applicationStatus);
    }

    public static VacancyDto publicCard(Vacancy v) {
        return card(v, null);
    }
}
