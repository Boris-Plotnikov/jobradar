package dev.plotnikov.jobradar.vacancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.plotnikov.jobradar.source.RawJob;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class NormalizationTest {

    private final SkillExtractor skills = new SkillExtractor();
    private final Normalizer normalizer = new Normalizer(new CountryResolver(), new LanguageDetector(), skills);
    private final Instant now = Instant.parse("2026-11-18T06:00:00Z");

    private RawJob raw(String title, String location, Boolean remote, String description) {
        return new RawJob("Test", "1", "https://example.com/1", title, "Example GmbH", location, remote, description,
                List.of(), null, null, null, null, null);
    }

    @Test
    void resolvesCountryAndCity() {
        CountryResolver r = new CountryResolver();
        assertThat(r.resolve("Zurich, Zürich, Switzerland", null)).isEqualTo(new CountryResolver.Place("CH", "Zurich"));
        assertThat(r.resolve("Wien", null).country()).isEqualTo("AT");
        assertThat(r.resolve("Remote - Europe", null).country()).isEqualTo("EU");
        assertThat(r.resolve("", "at").country()).isEqualTo("AT");
        assertThat(r.resolve("Mars", null).country()).isNull();
    }

    @Test
    void detectsGermanAndEnglish() {
        LanguageDetector d = new LanguageDetector();
        assertThat(d.detect("Wir suchen eine Entwicklerin für unser Team und die Zukunft")).isEqualTo("DE");
        assertThat(d.detect("We are looking for a developer to join our team and the future")).isEqualTo("EN");
    }

    @Test
    void extractsSkillsWithoutFalsePositives() {
        assertThat(skills.extract("Java, Spring Boot, PostgreSQL and C++ knowledge; Git"))
                .contains("Java", "Spring", "PostgreSQL", "C++", "Git");
        assertThat(skills.extract("JavaScript frontend with React")).doesNotContain("Java").contains("JavaScript", "React");
    }

    @Test
    void normalizesAFullJob() {
        Vacancy v = normalizer.normalize(raw("Junior Java Developer (m/w/d)", "Vienna, Austria", false,
                "Hybrid work. Java, Spring, SQL."), now);
        assertThat(v.getCountry()).isEqualTo("AT");
        assertThat(v.getCity()).isEqualTo("Vienna");
        assertThat(v.getWorkMode()).isEqualTo(WorkMode.ONSITE);
        assertThat(v.skillList()).contains("Java", "Spring", "SQL");
        assertThat(v.getPublishedAt()).isEqualTo(now);
        assertThat(v.getFingerprint()).hasSize(64);
    }

    @Test
    void workModeFromFlagsAndText() {
        assertThat(Normalizer.detectWorkMode(raw("Dev", "Berlin (Hybrid)", false, ""))).isEqualTo(WorkMode.HYBRID);
        assertThat(Normalizer.detectWorkMode(raw("Dev", "Berlin", true, ""))).isEqualTo(WorkMode.REMOTE);
        assertThat(Normalizer.detectWorkMode(raw("Dev", "Berlin", null, ""))).isEqualTo(WorkMode.ONSITE);
    }

    @Test
    void rejectsJobWithoutTitle() {
        assertThatThrownBy(() -> normalizer.normalize(raw(null, "Vienna", true, ""), now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fingerprintIgnoresGenderMarkersAndCompanyForm() {
        String a = Fingerprints.fingerprint("Acme GmbH", "Java Developer (m/w/d)", "DE");
        String b = Fingerprints.fingerprint("ACME", "Java Developer m/f/d", "DE");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void titleSimilarity() {
        assertThat(Fingerprints.titleSimilarity("Junior Java Developer", "Java Developer Junior")).isEqualTo(1.0);
        assertThat(Fingerprints.titleSimilarity("Java Java Developer", "Java Developer")).isEqualTo(1.0);
        assertThat(Fingerprints.titleSimilarity("Junior Java Developer", "Senior Python Engineer")).isLessThan(0.3);
    }
}
