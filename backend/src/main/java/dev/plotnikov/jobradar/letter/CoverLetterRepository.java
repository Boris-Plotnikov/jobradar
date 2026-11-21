package dev.plotnikov.jobradar.letter;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {

    List<CoverLetter> findByVacancyIdOrderByVersionDesc(Long vacancyId);

    Optional<CoverLetter> findFirstByVacancyIdAndLanguageOrderByVersionDesc(Long vacancyId, String language);

    long countByGeneratedBy(String generatedBy);
}
