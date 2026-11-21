package dev.plotnikov.jobradar.source;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ArbeitnowSource implements JobSource {
    static final String NAME = "Arbeitnow";
    private static final String URL = "https://www.arbeitnow.com/api/job-board-api?page=";
    private static final int PAGES = 3;

    private final RestClient http;

    public ArbeitnowSource(RestClient http) {
        this.http = http;
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
        List<RawJob> jobs = new ArrayList<>();
        for (int page = 1; page <= PAGES; page++) {
            JsonNode root = http.get().uri(URL + page).retrieve().body(JsonNode.class);
            List<RawJob> pageJobs = parse(root);
            jobs.addAll(pageJobs);
            if (pageJobs.isEmpty()) {
                break;
            }
        }
        return jobs;
    }

    public List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        if (root == null || !root.has("data")) {
            return jobs;
        }
        for (JsonNode item : root.get("data")) {
            List<String> tags = new ArrayList<>(JsonValues.strings(item, "tags"));
            tags.addAll(JsonValues.strings(item, "job_types"));
            Long created = item.has("created_at") ? item.get("created_at").asLong() : null;
            jobs.add(new RawJob(
                    NAME,
                    JsonValues.text(item, "slug"),
                    JsonValues.text(item, "url"),
                    JsonValues.text(item, "title"),
                    JsonValues.text(item, "company_name"),
                    JsonValues.text(item, "location"),
                    item.path("remote").asBoolean(false),
                    HtmlText.toPlain(JsonValues.text(item, "description")),
                    tags,
                    created == null ? null : Instant.ofEpochSecond(created),
                    null,
                    null,
                    null,
                    null));
        }
        return jobs;
    }
}
