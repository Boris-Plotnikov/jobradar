package dev.plotnikov.jobradar.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import dev.plotnikov.jobradar.source.ArbeitnowSource;
import dev.plotnikov.jobradar.source.HimalayasSource;
import dev.plotnikov.jobradar.source.JobSource;
import dev.plotnikov.jobradar.source.JobicySource;
import dev.plotnikov.jobradar.source.RawJob;
import dev.plotnikov.jobradar.source.RemotiveSource;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.VacancyRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PipelineServiceTest {
    @MockitoBean ArbeitnowSource arbeitnow;
    @MockitoBean RemotiveSource remotive;
    @MockitoBean JobicySource jobicy;
    @MockitoBean HimalayasSource himalayas;

    @Autowired PipelineService pipeline;
    @Autowired VacancyRepository vacancies;

    @TestConfiguration
    static class FakeSourceConfig {
        @Bean
        JobSource fakeSource() {
            return new JobSource() {
                @Override public String name() { return "Fake"; }
                @Override public boolean isEnabled() { return true; }
                @Override public List<RawJob> fetch() {
                    Instant date = Instant.parse("2026-11-17T10:00:00Z");
                    return List.of(
                            job("1", "Junior Java Developer (m/w/d)", "Acme GmbH", "Vienna, Austria",
                                    "We use Java, Spring Boot and PostgreSQL. 1-2 years of experience.", date),
                            job("2", "Junior Java Developer m/f/d", "ACME", "Wien",
                                    "Java and Spring.", date),
                            job("3", "Senior Java Architect", "Big Corp", "Berlin", "Java, 10+ years", date),
                            job("4", "Werkstudent Softwareentwicklung C++", "Robo AG", "Munich, Germany",
                                    "Wir suchen Unterstützung in C++ und Git. Du studierst Informatik.", date),
                            job("5", "Office Manager", "Shop", "Vienna", "Organize the office", date));
                }
            };
        }

        private static RawJob job(String id, String title, String company, String location, String text, Instant at) {
            return new RawJob("Fake", id, "https://example.com/" + id, title, company, location, false, text,
                    List.of(), at, null, null, null, null);
        }
    }

    @Test
    void fullRunFiltersDeduplicatesAndScores() {
        PipelineRun run = pipeline.run("MANUAL");

        assertThat(run.getStatus()).isEqualTo("OK");
        assertThat(run.getFetched()).isEqualTo(5);
        assertThat(run.getDuplicates()).isEqualTo(1);
        assertThat(run.getNewVacancies()).isEqualTo(4);
        assertThat(run.getPassedPrefilter()).isEqualTo(2);
        assertThat(run.getScored()).isEqualTo(2);

        List<Vacancy> passed = vacancies.findAll().stream().filter(Vacancy::isPrefilterPassed).toList();
        assertThat(passed).extracting(Vacancy::getTitle)
                .containsExactlyInAnyOrder("Junior Java Developer (m/w/d)", "Werkstudent Softwareentwicklung C++");
        assertThat(passed).allSatisfy(v -> {
            assertThat(v.getScoredBy()).isEqualTo("KEYWORDS");
            assertThat(v.getMatchScore()).isBetween(0, 100);
        });
        Vacancy german = passed.stream().filter(v -> v.getTitle().startsWith("Werkstudent")).findFirst().orElseThrow();
        assertThat(german.getLanguage()).isEqualTo("DE");
        assertThat(german.getCountry()).isEqualTo("DE");
    }

    @Test
    void secondRunSkipsKnownJobs() {
        pipeline.run("MANUAL");
        PipelineRun second = pipeline.run("MANUAL");
        assertThat(second.getNewVacancies()).isZero();
        assertThat(vacancies.count()).isEqualTo(4);
    }
}
