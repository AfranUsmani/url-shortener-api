package io.github.afranusmani.urlshortener.exception;

/**
 * Thrown when a short code exists but its link has passed its expiry time.
 * Mapped to HTTP 410 Gone.
 */
public class LinkExpiredException extends RuntimeException {

    public LinkExpiredException(String shortCode) {
        super("This link has expired: " + shortCode);
    }
}
