package dev.plotnikov.jobradar.vacancy;

public record VacancyFilter(
        String country,
        WorkMode workMode,
        String source,
        String direction,
        Integer minScore,
        String language,
        String search,
        Boolean includeRejected,
        Boolean includeHidden,
        Integer days) {
    public static VacancyFilter publicShowcase() {
        return new VacancyFilter(null, null, null, null, null, null, null, false, false, 14);
    }
}
