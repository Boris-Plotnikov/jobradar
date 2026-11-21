package dev.plotnikov.jobradar.application;

import java.time.Instant;

public record ApplicationDto(
        Long id,
        Long vacancyId,
        String title,
        String company,
        String country,
        Integer matchScore,
        String url,
        ApplicationStatus status,
        String notes,
        Instant appliedAt,
        Instant statusChangedAt,
        boolean needsFollowUp) {}
