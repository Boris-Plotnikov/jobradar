package dev.plotnikov.jobradar.source;

import java.time.Instant;
import java.util.List;

public record RawJob(
        String source,
        String externalId,
        String url,
        String title,
        String company,
        String location,
        Boolean remote,
        String description,
        List<String> tags,
        Instant publishedAt,
        Integer salaryMin,
        Integer salaryMax,
        String salaryCurrency,
        String countryHint) {}
