package dev.plotnikov.jobradar.vacancy;

import dev.plotnikov.jobradar.application.ApplicationRepository;
import dev.plotnikov.jobradar.application.JobApplication;
import dev.plotnikov.jobradar.common.NotFoundException;
import dev.plotnikov.jobradar.matching.MatchingService;
import dev.plotnikov.jobradar.settings.SettingsService;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VacancyService {
    static final int MAX_PAGE_SIZE = 100;

    private final VacancyRepository vacancies;
    private final ApplicationRepository applications;
    private final MatchingService matching;
    private final SettingsService settings;
    private final Clock clock;

    public VacancyService(VacancyRepository vacancies, ApplicationRepository applications, MatchingService matching,
                          SettingsService settings, Clock clock) {
        this.vacancies = vacancies;
        this.applications = applications;
        this.matching = matching;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<VacancyDto> feed(VacancyFilter filter, String sort, int page, int size) {
        Sort order = "date".equals(sort)
                ? Sort.by(Sort.Order.desc("fetchedAt"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.desc("matchScore"), Sort.Order.desc("fetchedAt"));
        PageRequest request = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), order);
        Page<Vacancy> result = vacancies.findAll(VacancySpecifications.fromFilter(filter, Instant.now(clock)), request);

        Map<Long, String> statuses = applications.findAll().stream()
                .collect(Collectors.toMap(a -> a.getVacancy().getId(), a -> a.getStatus().name(), (a, b) -> a));
        return result.map(v -> VacancyDto.card(v, statuses.get(v.getId())));
    }

    @Transactional(readOnly = true)
    public VacancyDto detail(Long id) {
        Vacancy v = find(id);
        String status = applications.findByVacancyId(id).map(JobApplication::getStatus).map(Enum::name).orElse(null);
        return VacancyDto.full(v, status);
    }

    @Transactional
    public VacancyDto setHidden(Long id, boolean hidden) {
        Vacancy v = find(id);
        v.setHidden(hidden);
        return detail(id);
    }

    @Transactional
    public VacancyDto rescore(Long id) {
        Vacancy v = find(id);
        matching.scoreAndApply(v, settings.get(), true);
        return detail(id);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> filterOptions() {
        return Map.of(
                "sources", vacancies.findAllSources(),
                "countries", vacancies.findAllCountries(),
                "directions", settings.get().directionList());
    }

    private Vacancy find(Long id) {
        return vacancies.findById(id).orElseThrow(() -> new NotFoundException("Vacancy " + id + " not found"));
    }

}
