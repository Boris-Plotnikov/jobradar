package dev.plotnikov.jobradar.vacancy;

import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vacancies")
public class VacancyController {

    private final VacancyService service;

    public VacancyController(VacancyService service) {
        this.service = service;
    }

    @GetMapping
    public Page<VacancyDto> feed(VacancyFilter filter,
                                 @RequestParam(defaultValue = "score") String sort,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "30") int size) {
        return service.feed(filter, sort, page, size);
    }

    @GetMapping("/filters")
    public Map<String, Object> filters() {
        return service.filterOptions();
    }

    @GetMapping("/{id}")
    public VacancyDto detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping("/{id}/hide")
    public VacancyDto hide(@PathVariable Long id) {
        return service.setHidden(id, true);
    }

    @PostMapping("/{id}/unhide")
    public VacancyDto unhide(@PathVariable Long id) {
        return service.setHidden(id, false);
    }

    @PostMapping("/{id}/rescore")
    public VacancyDto rescore(@PathVariable Long id) {
        return service.rescore(id);
    }
}
