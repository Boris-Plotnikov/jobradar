package dev.plotnikov.jobradar.application;

import dev.plotnikov.jobradar.common.NotFoundException;
import dev.plotnikov.jobradar.vacancy.Vacancy;
import dev.plotnikov.jobradar.vacancy.VacancyRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {
    static final Duration FOLLOW_UP_AFTER = Duration.ofDays(10);

    private final ApplicationRepository applications;
    private final VacancyRepository vacancies;
    private final Clock clock;

    public ApplicationService(ApplicationRepository applications, VacancyRepository vacancies, Clock clock) {
        this.applications = applications;
        this.vacancies = vacancies;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ApplicationDto> board() {
        return applications.findAllByOrderByStatusChangedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public ApplicationDto save(Long vacancyId) {
        Vacancy vacancy = vacancies.findById(vacancyId)
                .orElseThrow(() -> new NotFoundException("Vacancy " + vacancyId + " not found"));
        JobApplication app = applications.findByVacancyId(vacancyId).orElseGet(() -> {
            Instant now = Instant.now(clock);
            JobApplication created = new JobApplication();
            created.setVacancy(vacancy);
            created.setStatus(ApplicationStatus.SAVED);
            created.setCreatedAt(now);
            created.setStatusChangedAt(now);
            return applications.save(created);
        });
        return toDto(app);
    }

    @Transactional
    public ApplicationDto move(Long id, ApplicationStatus status) {
        JobApplication app = find(id);
        if (app.getStatus() != status) {
            Instant now = Instant.now(clock);
            app.setStatus(status);
            app.setStatusChangedAt(now);
            if (status == ApplicationStatus.APPLIED && app.getAppliedAt() == null) {
                app.setAppliedAt(now);
            }
        }
        return toDto(app);
    }

    @Transactional
    public ApplicationDto updateNotes(Long id, String notes) {
        JobApplication app = find(id);
        app.setNotes(notes);
        return toDto(app);
    }

    @Transactional
    public void delete(Long id) {
        applications.delete(find(id));
    }

    boolean needsFollowUp(JobApplication app) {
        return app.getStatus() == ApplicationStatus.APPLIED
                && app.getStatusChangedAt().plus(FOLLOW_UP_AFTER).isBefore(Instant.now(clock));
    }

    private JobApplication find(Long id) {
        return applications.findById(id).orElseThrow(() -> new NotFoundException("Application " + id + " not found"));
    }

    private ApplicationDto toDto(JobApplication a) {
        Vacancy v = a.getVacancy();
        return new ApplicationDto(a.getId(), v.getId(), v.getTitle(), v.getCompany(), v.getCountry(),
                v.getMatchScore(), v.getUrl(), a.getStatus(), a.getNotes(), a.getAppliedAt(),
                a.getStatusChangedAt(), needsFollowUp(a));
    }
}
