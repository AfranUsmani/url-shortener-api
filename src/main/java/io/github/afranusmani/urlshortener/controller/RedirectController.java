package io.github.afranusmani.urlshortener.controller;

import io.github.afranusmani.urlshortener.exception.LinkExpiredException;
import io.github.afranusmani.urlshortener.service.ResolvedUrl;
import io.github.afranusmani.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Handles the public redirect path {@code GET /{shortCode}}, resolving a code to
 * its original URL and issuing an HTTP 302. The code pattern allows the letters,
 * digits, {@code -} and {@code _} used by generated codes and custom aliases,
 * while still leaving dotted paths (static assets) to the resource handler.
 */
@RestController
@Tag(name = "Redirect", description = "Resolve a short code to its target URL")
public class RedirectController {

    private final UrlService service;

    public RedirectController(UrlService service) {
        this.service = service;
    }

    @GetMapping("/{shortCode:[0-9A-Za-z_-]+}")
    @Operation(summary = "Redirect a short code to its original URL")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer,
            @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {

        ResolvedUrl resolved = service.resolve(shortCode);
        if (resolved.isExpired()) {
            throw new LinkExpiredException(shortCode);
        }
        service.recordHit(shortCode, referer, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(resolved.originalUrl()))
                .build();
    }
}
