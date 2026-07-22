package io.github.afranusmani.urlshortener.dto;

import io.github.afranusmani.urlshortener.model.UrlMapping;

import java.time.Instant;

/**
 * Response payload describing a short link and its statistics.
 */
public record UrlResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        long hitCount,
        Instant createdAt
) {

    public static UrlResponse from(UrlMapping mapping, String baseUrl) {
        String shortUrl = baseUrl.replaceAll("/+$", "") + "/" + mapping.getShortCode();
        return new UrlResponse(
                mapping.getShortCode(),
                shortUrl,
                mapping.getOriginalUrl(),
                mapping.getHitCount(),
                mapping.getCreatedAt()
        );
    }
}
