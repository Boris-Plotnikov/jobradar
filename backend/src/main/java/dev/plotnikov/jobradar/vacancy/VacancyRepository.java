package dev.plotnikov.jobradar.vacancy;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface VacancyRepository extends JpaRepository<Vacancy, Long>, JpaSpecificationExecutor<Vacancy> {
    boolean existsBySourceAndExternalId(String source, String externalId);

    List<Vacancy> findByFingerprint(String fingerprint);

    List<Vacancy> findByCompanyIgnoreCaseAndFetchedAtAfter(String company, Instant after);

    List<Vacancy> findByPrefilterPassedTrueAndMatchScoreIsNullOrderByFetchedAtDesc();

    List<Vacancy> findByNotifiedFalseAndMatchScoreGreaterThanEqualOrderByMatchScoreDesc(int minScore);

    List<Vacancy> findByFetchedAtAfter(Instant after);

    long countByFetchedAtAfter(Instant after);

    long countByFetchedAtAfterAndMatchScoreGreaterThanEqual(Instant after, int score);

    @Query("select distinct v.source from Vacancy v order by v.source")
    List<String> findAllSources();

    @Query("select distinct v.country from Vacancy v where v.country is not null order by v.country")
    List<String> findAllCountries();
}
