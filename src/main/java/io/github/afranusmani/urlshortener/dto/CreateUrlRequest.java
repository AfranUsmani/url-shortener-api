package io.github.afranusmani.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a short link.
 *
 * @param url the original URL to shorten; must be a well-formed http(s) URL
 */
public record CreateUrlRequest(
        @NotBlank(message = "url must not be blank")
        @Size(max = 2048, message = "url must not exceed 2048 characters")
        @Pattern(
                regexp = "^https?://.+",
                message = "url must start with http:// or https://"
        )
        String url
) {
}
