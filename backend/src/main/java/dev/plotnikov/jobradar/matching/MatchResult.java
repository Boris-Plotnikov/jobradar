package dev.plotnikov.jobradar.matching;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record MatchResult(
        @JsonPropertyDescription("Match between 0 and 100. 80+ = apply now, 60-79 = worth applying, below 40 = poor fit")
        int score,
        @JsonPropertyDescription("One sentence in English: why this score")
        String summary,
        @JsonPropertyDescription("2-4 concrete strengths of the candidate for this job")
        List<String> strengths,
        @JsonPropertyDescription("0-4 concrete missing skills or requirements")
        List<String> gaps,
        @JsonPropertyDescription("One practical tip for the application, max 2 sentences")
        String advice) {
    public MatchResult validated() {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score out of range: " + score);
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Empty summary");
        }
        return new MatchResult(
                score,
                summary.trim(),
                strengths == null ? List.of() : strengths.stream().limit(5).toList(),
                gaps == null ? List.of() : gaps.stream().limit(5).toList(),
                advice == null ? "" : advice.trim());
    }
}
