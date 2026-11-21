package dev.plotnikov.jobradar.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByVacancyId(Long vacancyId);

    List<JobApplication> findAllByOrderByStatusChangedAtDesc();

    List<JobApplication> findByAppliedAtAfter(Instant after);

    long countByStatus(ApplicationStatus status);
}
