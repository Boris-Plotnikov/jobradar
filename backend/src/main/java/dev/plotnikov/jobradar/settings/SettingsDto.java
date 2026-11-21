package dev.plotnikov.jobradar.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SettingsDto(
        @NotBlank @Size(max = 20000) String resumeText,
        @NotBlank @Size(max = 5000) String directions,
        @NotBlank @Size(max = 1000) String countries,
        @NotBlank @Size(max = 5000) String keywords,
        @Size(max = 5000) String stopWords,
        @Min(0) @Max(100) int minScore,
        @Min(0) @Max(23) int digestHour,
        boolean remoteOnly) {
    public static SettingsDto from(Settings s) {
        return new SettingsDto(s.getResumeText(), s.getDirections(), s.getCountries(), s.getKeywords(),
                s.getStopWords(), s.getMinScore(), s.getDigestHour(), s.isRemoteOnly());
    }
}
