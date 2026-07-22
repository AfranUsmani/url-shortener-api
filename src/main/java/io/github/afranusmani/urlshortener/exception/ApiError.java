package io.github.afranusmani.urlshortener.exception;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error body returned for all handled exceptions.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        List<String> messages,
        String path
) {
}
