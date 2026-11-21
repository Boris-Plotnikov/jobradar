package dev.plotnikov.jobradar.pipeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "pipeline_runs")
public class PipelineRun {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant startedAt;
    private Instant finishedAt;
    private String triggerType;
    private String status;
    private int fetched;
    private int newVacancies;
    private int duplicates;
    private int passedPrefilter;
    private int scored;
    private int topMatches;

    @Column(columnDefinition = "TEXT")
    private String log = "";

    public void appendLog(String line) {
        log = log + line + "\n";
    }

    public Long getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getFetched() { return fetched; }
    public void setFetched(int fetched) { this.fetched = fetched; }
    public int getNewVacancies() { return newVacancies; }
    public void setNewVacancies(int newVacancies) { this.newVacancies = newVacancies; }
    public int getDuplicates() { return duplicates; }
    public void setDuplicates(int duplicates) { this.duplicates = duplicates; }
    public int getPassedPrefilter() { return passedPrefilter; }
    public void setPassedPrefilter(int passedPrefilter) { this.passedPrefilter = passedPrefilter; }
    public int getScored() { return scored; }
    public void setScored(int scored) { this.scored = scored; }
    public int getTopMatches() { return topMatches; }
    public void setTopMatches(int topMatches) { this.topMatches = topMatches; }
    public String getLog() { return log; }
}
