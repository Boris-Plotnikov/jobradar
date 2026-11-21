package dev.plotnikov.jobradar.source;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JobicySource implements JobSource {
    static final String NAME = "Jobicy";
    private static final String URL = "https://jobicy.com/api/v2/remote-jobs?count=50&geo=europe&industry=engineering";

    private final RestClient http;

    public JobicySource(RestClient http) {
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
        return parse(http.get().uri(URL).retrieve().body(JsonNode.class));
    }

    public List<RawJob> parse(JsonNode root) {
        List<RawJob> jobs = new ArrayList<>();
        if (root == null || !root.has("jobs")) {
            return jobs;
        }
        for (JsonNode item : root.get("jobs")) {
            List<String> tags = new ArrayList<>(JsonValues.strings(item, "jobIndustry"));
            tags.addAll(JsonValues.strings(item, "jobType"));
            String level = JsonValues.text(item, "jobLevel");
            if (level != null) {
                tags.add(level);
            }
            jobs.add(new RawJob(
                    NAME,
                    JsonValues.text(item, "id"),
                    JsonValues.text(item, "url"),
                    JsonValues.text(item, "jobTitle"),
                    JsonValues.text(item, "companyName"),
                    JsonValues.text(item, "jobGeo"),
                    true,
                    HtmlText.toPlain(JsonValues.text(item, "jobDescription")),
                    tags,
                    parseDate(JsonValues.text(item, "pubDate")),
                    JsonValues.integer(item, "salaryMin"),
                    JsonValues.integer(item, "salaryMax"),
                    JsonValues.text(item, "salaryCurrency"),
                    null));
        }
        return jobs;
    }

    static Instant parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
