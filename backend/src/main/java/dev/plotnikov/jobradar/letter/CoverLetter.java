package dev.plotnikov.jobradar.letter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cover_letters")
public class CoverLetter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vacancyId;
    private String language;
    private int version;

    @Column(columnDefinition = "TEXT")
    private String body;

    private String generatedBy;
    private Instant createdAt;

    public Long getId() { return id; }
    public Long getVacancyId() { return vacancyId; }
    public void setVacancyId(Long vacancyId) { this.vacancyId = vacancyId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
