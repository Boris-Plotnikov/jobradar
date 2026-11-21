package dev.plotnikov.jobradar.source;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class HimalayasSource implements JobSource {
    static final String NAME = "Himalayas";
    private static final Logger log = LoggerFactory.getLogger(HimalayasSource.class);
    private static final List<String> COUNTRIES = List.of("AT", "DE", "CH", "NL");
    private static final List<String> QUERIES = List.of("developer", "java");

    private final RestClient http;
    private final Clock clock;

    public HimalayasSource(RestClient http, Clock clock) {
        this.http = http;
        this.clock = clock;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<RawJob> fetch() {
        Map<String, RawJob> unique = new LinkedHashMap<>();
        for (String country : COUNTRIES) {
            for (String query : QUERIES) {
                String uri = UriComponentsBuilder.fromUriString("https://himalayas.app/jobs/api/search")
                        .queryParam("q", query)
                        .queryParam("country", country)
                        .queryParam("seniority", "Entry-level")
                        .queryParam("sort", "recent")
                        .toUriString();
                try {
                    JsonNode root = http.get().uri(uri).retrieve().body(JsonNode.class);
                    for (RawJob job : parse(root, country, Instant.now(clock))) {
                        unique.putIfAbsent(job.externalId(), job);
                    }
                } catch (RuntimeException e) {
                    log.warn("Himalayas {} '{}' failed: {}", country, query, e.getMessage());
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    public List<RawJob> parse(JsonNode root, String country, Instant now) {
        List<RawJob> jobs = new ArrayList<>();
        if (root == null || !root.has("jobs")) {
            return jobs;
        }
        for (JsonNode item : root.get("jobs")) {
            Instant expires = seconds(item, "expiryDate");
            if (expires != null && expires.isBefore(now)) {
                continue;
            }
            boolean annual = "annual".equals(item.path("salaryPeriod").asText("annual"));
            jobs.add(new RawJob(
                    NAME,
                    JsonValues.text(item, "guid"),
                    JsonValues.text(item, "applicationLink"),
                    JsonValues.text(item, "title"),
                    item.path("companyName").asText("Unknown company"),
                    item.path("locationRestrictions").isEmpty() ? "Worldwide" : null,
                    true,
                    HtmlText.toPlain(JsonValues.text(item, "description")),
                    JsonValues.strings(item, "categories"),
                    seconds(item, "pubDate"),
                    annual ? JsonValues.integer(item, "minSalary") : null,
                    annual ? JsonValues.integer(item, "maxSalary") : null,
                    JsonValues.text(item, "currency"),
                    country));
        }
        return jobs;
    }

    private static Instant seconds(JsonNode item, String field) {
        JsonNode value = item.get(field);
        if (value == null || value.isNull() || value.asLong(0) == 0) {
            return null;
        }
        return Instant.ofEpochSecond(value.asLong());
    }
}
