package dev.plotnikov.jobradar.matching;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    @Query("select coalesce(sum(u.costUsd), 0) from AiUsage u where u.createdAt >= :from")
    BigDecimal sumCostSince(@Param("from") Instant from);

    long countByCreatedAtAfter(Instant from);

    List<AiUsage> findByPurposeAndCreatedAtAfter(String purpose, Instant from);

    Optional<AiUsage> findFirstByVacancyIdAndPurposeOrderByCreatedAtDesc(Long vacancyId, String purpose);
}
