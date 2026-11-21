package dev.plotnikov.jobradar.vacancy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "vacancies")
public class Vacancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String externalId;
    private String url;
    private String title;
    private String company;
    private String country;
    private String city;

    @Enumerated(EnumType.STRING)
    private WorkMode workMode;

    private String language;
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String skills = "";
    private String direction;
    private Instant publishedAt;
    private Instant fetchedAt;
    private String fingerprint;

    private boolean prefilterPassed;
    private String prefilterReason;

    private Integer matchScore;
    private String matchSummary;

    @Column(columnDefinition = "TEXT")
    private String matchStrengths;

    @Column(columnDefinition = "TEXT")
    private String matchGaps;

    @Column(columnDefinition = "TEXT")
    private String matchAdvice;

    private String scoredBy;
    private Instant scoredAt;
    private boolean hidden;
    private boolean notified;

    public List<String> skillList() {
        if (skills == null || skills.isBlank()) {
            return List.of();
        }
        return Arrays.asList(skills.split(","));
    }

    public static List<String> lines(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public boolean isScored() {
        return matchScore != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public WorkMode getWorkMode() { return workMode; }
    public void setWorkMode(WorkMode workMode) { this.workMode = workMode; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public Integer getSalaryMin() { return salaryMin; }
    public void setSalaryMin(Integer salaryMin) { this.salaryMin = salaryMin; }
    public Integer getSalaryMax() { return salaryMax; }
    public void setSalaryMax(Integer salaryMax) { this.salaryMax = salaryMax; }
    public String getSalaryCurrency() { return salaryCurrency; }
    public void setSalaryCurrency(String salaryCurrency) { this.salaryCurrency = salaryCurrency; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public boolean isPrefilterPassed() { return prefilterPassed; }
    public void setPrefilterPassed(boolean prefilterPassed) { this.prefilterPassed = prefilterPassed; }
    public String getPrefilterReason() { return prefilterReason; }
    public void setPrefilterReason(String prefilterReason) { this.prefilterReason = prefilterReason; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getMatchSummary() { return matchSummary; }
    public void setMatchSummary(String matchSummary) { this.matchSummary = matchSummary; }
    public String getMatchStrengths() { return matchStrengths; }
    public void setMatchStrengths(String matchStrengths) { this.matchStrengths = matchStrengths; }
    public String getMatchGaps() { return matchGaps; }
    public void setMatchGaps(String matchGaps) { this.matchGaps = matchGaps; }
    public String getMatchAdvice() { return matchAdvice; }
    public void setMatchAdvice(String matchAdvice) { this.matchAdvice = matchAdvice; }
    public String getScoredBy() { return scoredBy; }
    public void setScoredBy(String scoredBy) { this.scoredBy = scoredBy; }
    public Instant getScoredAt() { return scoredAt; }
    public void setScoredAt(Instant scoredAt) { this.scoredAt = scoredAt; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
}
