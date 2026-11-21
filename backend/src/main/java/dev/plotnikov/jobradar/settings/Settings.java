package dev.plotnikov.jobradar.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "settings")
public class Settings {
    public static final long SINGLE_ID = 1L;

    @Id
    private Long id = SINGLE_ID;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(columnDefinition = "TEXT")
    private String directions;

    @Column(columnDefinition = "TEXT")
    private String countries;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(columnDefinition = "TEXT")
    private String stopWords;

    private int minScore;
    private int digestHour;
    private boolean remoteOnly;
    private Instant updatedAt;

    public static List<String> lines(String text) {
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(text.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<String> directionList() { return lines(directions); }
    public List<String> countryList() { return lines(countries).stream().map(c -> c.toUpperCase(Locale.ROOT)).toList(); }
    public List<String> keywordList() { return lines(keywords).stream().map(k -> k.toLowerCase(Locale.ROOT)).toList(); }
    public List<String> stopWordList() { return lines(stopWords).stream().map(k -> k.toLowerCase(Locale.ROOT)).toList(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public String getDirections() { return directions; }
    public void setDirections(String directions) { this.directions = directions; }
    public String getCountries() { return countries; }
    public void setCountries(String countries) { this.countries = countries; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getStopWords() { return stopWords; }
    public void setStopWords(String stopWords) { this.stopWords = stopWords; }
    public int getMinScore() { return minScore; }
    public void setMinScore(int minScore) { this.minScore = minScore; }
    public int getDigestHour() { return digestHour; }
    public void setDigestHour(int digestHour) { this.digestHour = digestHour; }
    public boolean isRemoteOnly() { return remoteOnly; }
    public void setRemoteOnly(boolean remoteOnly) { this.remoteOnly = remoteOnly; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
