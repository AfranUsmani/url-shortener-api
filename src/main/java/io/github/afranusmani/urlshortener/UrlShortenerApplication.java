package io.github.afranusmani.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the URL Shortener service.
 *
 * <p>Exposes a REST API to create short links and resolve them back to their
 * original URLs, with a cache-aside lookup path and Prometheus-ready metrics.
 */
@SpringBootApplication
@EnableCaching
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
