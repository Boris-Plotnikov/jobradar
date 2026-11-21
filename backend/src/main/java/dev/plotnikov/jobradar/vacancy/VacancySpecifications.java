package dev.plotnikov.jobradar.vacancy;

import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class VacancySpecifications {
    private VacancySpecifications() {}

    public static Specification<Vacancy> fromFilter(VacancyFilter f, Instant now) {
        return (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (!Boolean.TRUE.equals(f.includeRejected())) {
                conditions.add(cb.isTrue(root.get("prefilterPassed")));
            }
            if (!Boolean.TRUE.equals(f.includeHidden())) {
                conditions.add(cb.isFalse(root.get("hidden")));
            }
            if (f.country() != null && !f.country().isBlank()) {
                conditions.add(cb.equal(root.get("country"), f.country().toUpperCase(Locale.ROOT)));
            }
            if (f.workMode() != null) {
                conditions.add(cb.equal(root.get("workMode"), f.workMode()));
            }
            if (f.source() != null && !f.source().isBlank()) {
                conditions.add(cb.equal(root.get("source"), f.source()));
            }
            if (f.direction() != null && !f.direction().isBlank()) {
                conditions.add(cb.equal(root.get("direction"), f.direction()));
            }
            if (f.language() != null && !f.language().isBlank()) {
                conditions.add(cb.equal(root.get("language"), f.language().toUpperCase(Locale.ROOT)));
            }
            if (f.minScore() != null && f.minScore() > 0) {
                conditions.add(cb.greaterThanOrEqualTo(root.get("matchScore"), f.minScore()));
            }
            if (f.days() != null && f.days() > 0) {
                conditions.add(cb.greaterThanOrEqualTo(root.get("fetchedAt"), now.minus(Duration.ofDays(f.days()))));
            }
            if (f.search() != null && !f.search().isBlank()) {
                String like = "%" + f.search().toLowerCase(Locale.ROOT) + "%";
                conditions.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("company")), like),
                        cb.like(cb.lower(root.get("skills")), like)));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };
    }
}
