package io.github.afranusmani.urlshortener.repository;

import io.github.afranusmani.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Atomically increments the hit counter in a single statement, avoiding a
     * read-modify-write race on the hot redirect path.
     */
    @Modifying
    @Query("update UrlMapping u set u.hitCount = u.hitCount + 1 where u.shortCode = :shortCode")
    int incrementHitCount(@Param("shortCode") String shortCode);
}
