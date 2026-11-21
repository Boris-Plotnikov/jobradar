package dev.plotnikov.jobradar.matching;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plotnikov.jobradar.settings.Settings;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.WorkMode;
import org.junit.jupiter.api.Test;

class PrefilterTest {
    private final Prefilter prefilter = new Prefilter();

    static Settings settings() {
        Settings s = new Settings();
        s.setResumeText("Java, Spring Boot, SQL, C++, Git");
        s.setDirections("Junior Java Developer\nWorking Student Software Development");
        s.setCountries("AT\nDE\nEU");
        s.setKeywords("java\nwerkstudent\njunior");
        s.setStopWords("senior\nlead");
        s.setMinScore(60);
        return s;
    }

    static Vacancy vacancy(String title, String country, WorkMode mode, String description) {
        Vacancy v = new Vacancy();
        v.setTitle(title);
        v.setCompany("Acme");
        v.setCountry(country);
        v.setWorkMode(mode);
        v.setDescription(description);
        v.setSkills("Java,Spring");
        v.setLanguage("EN");
        return v;
    }

    @Test
    void passesJuniorJavaInAustria() {
        PrefilterResult r = prefilter.check(vacancy("Junior Java Developer", "AT", WorkMode.ONSITE, "2 years"), settings());
        assertThat(r.passed()).isTrue();
        assertThat(r.direction()).isEqualTo("Junior Java Developer");
    }

    @Test
    void rejectsStopWordInTitleOnlyAsWholeWord() {
        assertThat(prefilter.check(vacancy("Senior Java Developer", "AT", WorkMode.ONSITE, ""), settings()).reason())
                .contains("senior");
        assertThat(prefilter.check(vacancy("Junior Java Developer at a leading bank", "AT", WorkMode.ONSITE, ""),
                settings()).passed()).isTrue();
    }

    @Test
    void rejectsTooManyYears() {
        PrefilterResult r = prefilter.check(vacancy("Java Developer", "DE", WorkMode.ONSITE,
                "You have 5+ years of experience with Java"), settings());
        assertThat(r.passed()).isFalse();
        assertThat(r.reason()).contains("5+");
    }

    @Test
    void countryRules() {
        assertThat(prefilter.check(vacancy("Java Developer", "FR", WorkMode.ONSITE, ""), settings()).passed()).isFalse();
        assertThat(prefilter.check(vacancy("Java Developer", "EU", WorkMode.REMOTE, ""), settings()).passed()).isTrue();
        assertThat(prefilter.check(vacancy("Java Developer", null, WorkMode.REMOTE, ""), settings()).passed()).isTrue();
        assertThat(prefilter.check(vacancy("Java Developer", null, WorkMode.ONSITE, ""), settings()).passed()).isFalse();
    }

    @Test
    void needsAKeyword() {
        Vacancy v = vacancy("Marketing Manager", "AT", WorkMode.ONSITE, "Social media");
        v.setSkills("");
        assertThat(prefilter.check(v, settings()).reason()).isEqualTo("No search keyword found");
    }

    @Test
    void remoteOnlySetting() {
        Settings s = settings();
        s.setRemoteOnly(true);
        assertThat(prefilter.check(vacancy("Junior Java Developer", "AT", WorkMode.HYBRID, ""), s).passed()).isFalse();
    }

    @Test
    void requiredYearsTakesTheHighestNumber() {
        assertThat(Prefilter.requiredYears("1-2 years with Java, 4 years overall")).isEqualTo(4);
        assertThat(Prefilter.requiredYears("mindestens 3 Jahre Erfahrung")).isEqualTo(3);
        assertThat(Prefilter.requiredYears("no experience needed")).isNull();
    }
}
