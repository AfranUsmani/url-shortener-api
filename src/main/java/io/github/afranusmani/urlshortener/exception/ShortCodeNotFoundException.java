package io.github.afranusmani.urlshortener.exception;

/**
 * Thrown when a requested short code has no corresponding mapping.
 */
public class ShortCodeNotFoundException extends RuntimeException {

    public ShortCodeNotFoundException(String shortCode) {
        super("No URL found for short code: " + shortCode);
    }
}
