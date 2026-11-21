package dev.plotnikov.jobradar.source;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RemotiveSource implements JobSource {
    static final String NAME = "Remotive";
    private static final String URL = "https://remotive.com/api/remote-jobs?category=software-dev&limit=100";

    private final RestClient http;

    public RemotiveSource(RestClient http) {
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
            jobs.add(new RawJob(
                    NAME,
                    JsonValues.text(item, "id"),
                    JsonValues.text(item, "url"),
                    JsonValues.text(item, "title"),
                    JsonValues.text(item, "company_name"),
                    JsonValues.text(item, "candidate_required_location"),
                    true,
                    HtmlText.toPlain(JsonValues.text(item, "description")),
                    JsonValues.strings(item, "tags"),
                    parseDate(JsonValues.text(item, "publication_date")),
                    null,
                    null,
                    null,
                    null));
        }
        return jobs;
    }

    static Instant parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
