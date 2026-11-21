package dev.plotnikov.jobradar.pipeline;

import dev.plotnikov.jobradar.config.JobRadarProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PipelineScheduler {
    private static final Logger log = LoggerFactory.getLogger(PipelineScheduler.class);

    private final PipelineService pipeline;
    private final JobRadarProperties properties;

    public PipelineScheduler(PipelineService pipeline, JobRadarProperties properties) {
        this.pipeline = pipeline;
        this.properties = properties;
    }

    @Scheduled(cron = "${jobradar.pipeline.cron}", zone = "${jobradar.timezone}")
    public void daily() {
        if (!properties.pipeline().enabled()) {
            log.info("Pipeline schedule is disabled");
            return;
        }
        if (pipeline.isRunning()) {
            return;
        }
        pipeline.run("SCHEDULE");
    }
}
