package dev.plotnikov.jobradar.pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineController {
    private final PipelineService pipeline;
    private final PipelineRunRepository runs;

    public PipelineController(PipelineService pipeline, PipelineRunRepository runs) {
        this.pipeline = pipeline;
        this.runs = runs;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> run() {
        if (pipeline.isRunning()) {
            return ResponseEntity.status(409).body(Map.of("error", "The pipeline is already running"));
        }
        CompletableFuture.runAsync(() -> pipeline.run("MANUAL"));
        return ResponseEntity.accepted().body(Map.of("status", "started"));
    }

    @PostMapping("/refilter")
    public ResponseEntity<Map<String, String>> refilter() {
        if (pipeline.isRunning()) {
            return ResponseEntity.status(409).body(Map.of("error", "The pipeline is already running"));
        }
        CompletableFuture.runAsync(pipeline::reapplyFilters);
        return ResponseEntity.accepted().body(Map.of("status", "started"));
    }

    @GetMapping("/runs")
    public Map<String, Object> runs() {
        List<PipelineRun> recent = runs.findTop20ByOrderByStartedAtDesc();
        return Map.of("running", pipeline.isRunning(), "runs", recent);
    }
}
