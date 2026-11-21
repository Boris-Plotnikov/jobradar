package dev.plotnikov.jobradar.vacancy;

import dev.plotnikov.jobradar.source.HtmlText;
import dev.plotnikov.jobradar.source.RawJob;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class Normalizer {
    static final int MAX_DESCRIPTION = 8000;

    private final CountryResolver countryResolver;
    private final LanguageDetector languageDetector;
    private final SkillExtractor skillExtractor;

    public Normalizer(CountryResolver countryResolver, LanguageDetector languageDetector, SkillExtractor skillExtractor) {
        this.countryResolver = countryResolver;
        this.languageDetector = languageDetector;
        this.skillExtractor = skillExtractor;
    }

    public Vacancy normalize(RawJob raw, Instant now) {
        if (raw.externalId() == null || raw.url() == null || raw.title() == null) {
            throw new IllegalArgumentException("Job without id, url or title from " + raw.source());
        }
        Vacancy v = new Vacancy();
        v.setSource(raw.source());
        v.setExternalId(cut(raw.externalId(), 200));
        v.setUrl(cut(raw.url(), 1000));
        v.setTitle(cut(raw.title().trim(), 300));
        v.setCompany(cut(raw.company() == null || raw.company().isBlank() ? "Unknown company" : raw.company().trim(), 200));

        CountryResolver.Place place = countryResolver.resolve(raw.location(), raw.countryHint());
        v.setCountry(place.country());
        v.setCity(place.city());
        v.setWorkMode(detectWorkMode(raw));

        String description = HtmlText.limit(raw.description() == null ? "" : raw.description(), MAX_DESCRIPTION);
        v.setDescription(description);
        v.setLanguage(languageDetector.detect(raw.title() + "\n" + description));

        String skillText = raw.title() + "\n" + String.join(" ", raw.tags()) + "\n" + description;
        v.setSkills(cut(String.join(",", skillExtractor.extract(skillText)), 1000));

        v.setSalaryMin(raw.salaryMin());
        v.setSalaryMax(raw.salaryMax());
        v.setSalaryCurrency(raw.salaryCurrency());
        v.setPublishedAt(raw.publishedAt() != null ? raw.publishedAt() : now);
        v.setFetchedAt(now);
        v.setFingerprint(Fingerprints.fingerprint(v.getCompany(), v.getTitle(), v.getCountry()));
        return v;
    }

    static WorkMode detectWorkMode(RawJob raw) {
        String text = ((raw.location() == null ? "" : raw.location()) + " " + raw.title() + " "
                + String.join(" ", raw.tags())).toLowerCase(Locale.ROOT);
        if (text.contains("hybrid")) {
            return WorkMode.HYBRID;
        }
        if (Boolean.TRUE.equals(raw.remote()) || text.contains("remote") || text.contains("homeoffice")) {
            return WorkMode.REMOTE;
        }
        return WorkMode.ONSITE;
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
