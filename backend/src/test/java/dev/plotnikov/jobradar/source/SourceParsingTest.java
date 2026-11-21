package dev.plotnikov.jobradar.source;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SourceParsingTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient unused = RestClient.create();

    private JsonNode fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/" + name)) {
            return mapper.readTree(in);
        }
    }

    @Test
    void arbeitnow() throws IOException {
        List<RawJob> jobs = new ArbeitnowSource(unused).parse(fixture("arbeitnow.json"));
        assertThat(jobs).hasSize(3);
        RawJob first = jobs.get(0);
        assertThat(first.source()).isEqualTo("Arbeitnow");
        assertThat(first.externalId()).isNotBlank();
        assertThat(first.url()).startsWith("https://");
        assertThat(first.description()).doesNotContain("<p>");
        assertThat(first.publishedAt()).isNotNull();
    }

    @Test
    void remotive() throws IOException {
        List<RawJob> jobs = new RemotiveSource(unused).parse(fixture("remotive.json"));
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).remote()).isTrue();
        assertThat(jobs.get(0).publishedAt()).isNotNull();
    }

    @Test
    void jobicyWithSalary() throws IOException {
        List<RawJob> jobs = new JobicySource(unused).parse(fixture("jobicy.json"));
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).salaryMin()).isPositive();
        assertThat(jobs.get(0).salaryCurrency()).isEqualTo("EUR");
    }

    @Test
    void himalayasSkipsExpiredJobsAndMarksWorldwide() throws IOException {
        List<RawJob> jobs = new HimalayasSource(unused, Clock.systemUTC())
                .parse(fixture("himalayas.json"), "AT", Instant.parse("2026-11-20T00:00:00Z"));
        assertThat(jobs).hasSize(3);
        assertThat(jobs.get(0).source()).isEqualTo("Himalayas");
        assertThat(jobs.get(0).url()).contains("himalayas.app");
        assertThat(jobs.get(0).location()).isEqualTo("Worldwide");
        assertThat(jobs.get(0).countryHint()).isEqualTo("AT");
        assertThat(jobs.get(0).salaryMin()).isNull();
        assertThat(jobs.get(0).description()).doesNotContain("<p>");
    }

    @Test
    void brokenOrEmptyAnswersGiveEmptyList() throws IOException {
        assertThat(new RemotiveSource(unused).parse(mapper.readTree("{}"))).isEmpty();
        assertThat(new ArbeitnowSource(unused).parse(null)).isEmpty();
    }

    @Test
    void htmlToPlainText() {
        String plain = HtmlText.toPlain("<p>Hello&nbsp;<b>world</b></p><ul><li>Java</li><li>SQL</li></ul>");
        assertThat(plain).isEqualTo("Hello world\n- Java\n- SQL");
    }
}
