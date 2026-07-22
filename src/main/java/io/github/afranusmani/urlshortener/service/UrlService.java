package io.github.afranusmani.urlshortener.service;

import io.github.afranusmani.urlshortener.exception.ShortCodeNotFoundException;
import io.github.afranusmani.urlshortener.model.UrlMapping;
import io.github.afranusmani.urlshortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core business logic for creating and resolving short links.
 *
 * <p>The read path ({@link #resolve(String)}) is cache-aside: hot short codes
 * are served from the cache and only miss through to the database, which keeps
 * redirect latency low under load.
 */
@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    private final UrlRepository repository;

    public UrlService(UrlRepository repository) {
        this.repository = repository;
    }

    /**
     * Creates a new short link. The short code is derived from the persisted row
     * id (see {@link UrlMapping#assignShortCode()}), so it is unique by construction.
     */
    @Transactional
    public UrlMapping create(String originalUrl) {
        UrlMapping mapping = repository.save(new UrlMapping(originalUrl));
        log.info("Created short code '{}' for url '{}'", mapping.getShortCode(), originalUrl);
        return mapping;
    }

    /**
     * Resolves a short code to its original URL. Results are cached; the not-found
     * case is not cached because the exception short-circuits {@code @Cacheable}.
     */
    @Cacheable(cacheNames = "urls", key = "#shortCode")
    @Transactional(readOnly = true)
    public String resolve(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(UrlMapping::getOriginalUrl)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }

    /**
     * Records a redirect hit. Kept separate from {@link #resolve(String)} so the
     * cached read path is not invalidated by the write.
     */
    @Transactional
    public void recordHit(String shortCode) {
        repository.incrementHitCount(shortCode);
    }

    @Transactional(readOnly = true)
    public UrlMapping getMapping(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }
}
