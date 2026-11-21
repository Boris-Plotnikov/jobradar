package dev.plotnikov.jobradar.pipeline;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineRunRepository extends JpaRepository<PipelineRun, Long> {

    List<PipelineRun> findTop20ByOrderByStartedAtDesc();

    Optional<PipelineRun> findFirstByStatusOrderByStartedAtDesc(String status);

    List<PipelineRun> findByStartedAtAfterOrderByStartedAt(Instant after);
}
