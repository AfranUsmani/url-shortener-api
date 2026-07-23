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
        Instant createdAt,
        Instant expiresAt,
        boolean expired,
        String qrCodeUrl
) {

    public static UrlResponse from(UrlMapping mapping, String baseUrl) {
        String base = baseUrl.replaceAll("/+$", "");
        String shortUrl = base + "/" + mapping.getShortCode();
        String qrCodeUrl = base + "/api/v1/urls/" + mapping.getShortCode() + "/qr";
        return new UrlResponse(
                mapping.getShortCode(),
                shortUrl,
                mapping.getOriginalUrl(),
                mapping.getHitCount(),
                mapping.getCreatedAt(),
                mapping.getExpiresAt(),
                mapping.isExpired(),
                qrCodeUrl
        );
    }
}
