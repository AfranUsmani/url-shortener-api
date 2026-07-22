package io.github.afranusmani.urlshortener.controller;

import io.github.afranusmani.urlshortener.dto.CreateUrlRequest;
import io.github.afranusmani.urlshortener.dto.UrlResponse;
import io.github.afranusmani.urlshortener.model.UrlMapping;
import io.github.afranusmani.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URLs", description = "Create and inspect short links")
public class UrlController {

    private final UrlService service;
    private final String baseUrl;

    public UrlController(UrlService service, @Value("${app.base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    @Operation(summary = "Create a short link for a given URL")
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        UrlMapping mapping = service.create(request.url());
        UrlResponse body = UrlResponse.from(mapping, baseUrl);
        URI location = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/v1/urls/{code}")
                .buildAndExpand(mapping.getShortCode())
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Fetch metadata and hit statistics for a short link")
    public ResponseEntity<UrlResponse> stats(@PathVariable String shortCode) {
        UrlMapping mapping = service.getMapping(shortCode);
        return ResponseEntity.ok(UrlResponse.from(mapping, baseUrl));
    }
}
