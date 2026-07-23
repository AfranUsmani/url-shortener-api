package io.github.afranusmani.urlshortener.repository;

import io.github.afranusmani.urlshortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortCode(String shortCode);

    /**
     * Recent events for a short code, used to build the analytics breakdowns.
     * Aggregation is done in the service (in Java) to stay portable across H2
     * and PostgreSQL rather than relying on database-specific date functions.
     */
    List<ClickEvent> findByShortCodeAndOccurredAtGreaterThanEqual(String shortCode, Instant since);
}
