package io.github.afranusmani.urlshortener.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Sends the base URL to the interactive API docs so the landing page is useful
 * instead of a bare 404.
 */
@Hidden
@RestController
public class HomeController {

    @GetMapping("/")
    public ResponseEntity<Void> home() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui.html"))
                .build();
    }
}
