package io.github.afranusmani.urlshortener.service;

import java.io.Serializable;
import java.time.Instant;

/**
 * Cached resolution of a short code. Carries the expiry alongside the target so
 * that expiry is evaluated on <em>every</em> redirect — including cache hits —
 * rather than only when the entry is (re)loaded from the database.
 */
public record ResolvedUrl(String originalUrl, Instant expiresAt) implements Serializable {

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
