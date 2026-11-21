package dev.plotnikov.jobradar.insights;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InsightsController {

    private final InsightsService service;

    public InsightsController(InsightsService service) {
        this.service = service;
    }

    @GetMapping("/insights")
    public InsightsDtos.Insights insights(@RequestParam(defaultValue = "30") int days) {
        return service.insights(days);
    }

    @GetMapping("/stats")
    public InsightsDtos.Stats stats() {
        return service.stats();
    }
}
