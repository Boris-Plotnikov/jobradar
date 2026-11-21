package dev.plotnikov.jobradar.showcase;

import dev.plotnikov.jobradar.insights.InsightsDtos;
import dev.plotnikov.jobradar.insights.InsightsService;
import dev.plotnikov.jobradar.vacancy.VacancyDto;
import dev.plotnikov.jobradar.vacancy.VacancyFilter;
import dev.plotnikov.jobradar.vacancy.VacancyService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class ShowcaseController {
    private final VacancyService vacancies;
    private final InsightsService insights;
    private final AiTraceService aiTrace;

    public ShowcaseController(VacancyService vacancies, InsightsService insights, AiTraceService aiTrace) {
        this.vacancies = vacancies;
        this.insights = insights;
        this.aiTrace = aiTrace;
    }

    @GetMapping("/summary")
    public InsightsDtos.Summary summary() {
        return insights.summary();
    }

    @GetMapping("/feed")
    public List<VacancyDto> feed() {
        return vacancies.feed(VacancyFilter.publicShowcase(), "score", 0, 30).getContent().stream().map(this::withoutPersonalData).toList();
    }

    @GetMapping("/insights")
    public InsightsDtos.Insights insights() {
        return insights.insights(30);
    }

    @GetMapping("/ai-trace")
    public AiTrace aiTrace(@RequestParam(required = false) Long vacancyId) {
        return aiTrace.trace(vacancyId);
    }

    private VacancyDto withoutPersonalData(VacancyDto v) {
        return new VacancyDto(v.id(), v.source(), v.url(), v.title(), v.company(), v.country(), v.city(),
                v.workMode(), v.language(), v.salaryMin(), v.salaryMax(), v.salaryCurrency(), v.skills(),
                v.direction(), v.publishedAt(), v.fetchedAt(), v.prefilterPassed(), null, v.matchScore(),
                v.matchSummary(), null, null, null, v.scoredBy(), false, null, null);
    }
}
