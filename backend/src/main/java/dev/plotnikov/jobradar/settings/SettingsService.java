package dev.plotnikov.jobradar.settings;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final SettingsRepository repository;
    private final Clock clock;

    public SettingsService(SettingsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Settings get() {
        return repository.findById(Settings.SINGLE_ID)
                .orElseThrow(() -> new IllegalStateException("Settings row is missing - check migration V2"));
    }

    @Transactional
    public Settings update(SettingsDto dto) {
        Settings s = get();
        s.setResumeText(dto.resumeText().trim());
        s.setDirections(dto.directions().trim());
        s.setCountries(dto.countries().trim());
        s.setKeywords(dto.keywords().trim());
        s.setStopWords(dto.stopWords() == null ? "" : dto.stopWords().trim());
        s.setMinScore(dto.minScore());
        s.setDigestHour(dto.digestHour());
        s.setRemoteOnly(dto.remoteOnly());
        s.setUpdatedAt(Instant.now(clock));
        return repository.save(s);
    }
}
