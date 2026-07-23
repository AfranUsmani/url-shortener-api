package io.github.afranusmani.urlshortener.service;

import io.github.afranusmani.urlshortener.dto.AnalyticsResponse;
import io.github.afranusmani.urlshortener.exception.AliasAlreadyExistsException;
import io.github.afranusmani.urlshortener.exception.ShortCodeNotFoundException;
import io.github.afranusmani.urlshortener.model.UrlMapping;
import io.github.afranusmani.urlshortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Set;

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

    /**
     * Aliases that would shadow first-class application routes. Compared
     * case-insensitively so, e.g., {@code API} is rejected too.
     */
    private static final Set<String> RESERVED_ALIASES = Set.of(
            "api", "actuator", "swagger-ui", "h2-console", "v3", "webjars",
            "error", "favicon", "index", "assets", "static", "qr", "analytics",
            "robots", "sitemap", "login", "admin"
    );

    private final UrlRepository repository;
    private final ClickAnalyticsService clickAnalytics;

    public UrlService(UrlRepository repository, ClickAnalyticsService clickAnalytics) {
        this.repository = repository;
        this.clickAnalytics = clickAnalytics;
    }

    /**
     * Creates a new short link.
     *
     * <p>Without a custom alias the short code is derived from the persisted row
     * id (see {@link UrlMapping#assignShortCode()}), so it is unique by
     * construction. With a custom alias, availability is checked up front for a
     * friendly 409, and the unique index backs it up against races.
     */
    @Transactional
    public UrlMapping create(String originalUrl, String customAlias, Instant expiresAt) {
        UrlMapping mapping = new UrlMapping(originalUrl, expiresAt);

        if (StringUtils.hasText(customAlias)) {
            String alias = customAlias.trim();
            ensureAliasAllowed(alias);
            if (repository.existsByShortCode(alias)) {
                throw new AliasAlreadyExistsException(alias);
            }
            mapping.setShortCode(alias);
        }

        UrlMapping saved = repository.save(mapping);
        log.info("Created short code '{}' for url '{}'{}",
                saved.getShortCode(), originalUrl,
                expiresAt != null ? " (expires " + expiresAt + ")" : "");
        return saved;
    }

    private void ensureAliasAllowed(String alias) {
        if (RESERVED_ALIASES.contains(alias.toLowerCase())) {
            throw new IllegalArgumentException("customAlias '" + alias + "' is reserved");
        }
    }

    /**
     * Resolves a short code to its target (plus expiry). Results are cached; the
     * not-found case is not cached because the exception short-circuits
     * {@code @Cacheable}. Expiry is carried in the cached value so it is checked
     * by the caller on every redirect, not just on cache misses.
     */
    @Cacheable(cacheNames = "urls", key = "#shortCode")
    @Transactional(readOnly = true)
    public ResolvedUrl resolve(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(m -> new ResolvedUrl(m.getOriginalUrl(), m.getExpiresAt()))
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }

    /**
     * Records a redirect hit. The atomic counter bump is synchronous (a single
     * cheap UPDATE); the detailed analytics event is captured asynchronously so
     * it never adds latency to the redirect.
     */
    @Transactional
    public void recordHit(String shortCode, String referer, String userAgent) {
        repository.incrementHitCount(shortCode);
        clickAnalytics.record(shortCode, referer, userAgent);
    }

    @Transactional(readOnly = true)
    public UrlMapping getMapping(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }

    /** Aggregated analytics for a code; 404s if the code does not exist. */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        if (!repository.existsByShortCode(shortCode)) {
            throw new ShortCodeNotFoundException(shortCode);
        }
        return clickAnalytics.summarize(shortCode);
    }
}
