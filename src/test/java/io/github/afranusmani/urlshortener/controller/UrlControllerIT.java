package io.github.afranusmani.urlshortener.controller;

import io.github.afranusmani.urlshortener.dto.CreateUrlRequest;
import io.github.afranusmani.urlshortener.dto.UrlResponse;
import io.github.afranusmani.urlshortener.exception.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test running against a real embedded server, exercising
 * the full create -> redirect -> stats flow plus the Prometheus scrape endpoint
 * over actual HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability  // enable metrics export (disabled by default in tests) so /actuator/prometheus is served
class UrlControllerIT {

    @Autowired
    private TestRestTemplate rest;

    @BeforeEach
    void disableRedirectFollowing() {
        // Assert the 302 ourselves instead of chasing the redirect to the internet.
        rest.getRestTemplate().setRequestFactory(new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                    throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        });
    }

    @Test
    void createResolveAndTrackHits() {
        String target = "https://spring.io/projects/spring-boot";

        // Create a short link.
        ResponseEntity<UrlResponse> created =
                rest.postForEntity("/api/v1/urls", new CreateUrlRequest(target), UrlResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().shortCode()).isNotBlank();
        assertThat(created.getBody().originalUrl()).isEqualTo(target);
        assertThat(created.getBody().hitCount()).isZero();

        String shortCode = created.getBody().shortCode();

        // Redirect resolves to the original URL with a 302.
        ResponseEntity<Void> redirect = rest.getForEntity("/" + shortCode, Void.class);
        assertThat(redirect.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirect.getHeaders().getLocation()).isEqualTo(URI.create(target));

        // The redirect is reflected in the hit statistics.
        ResponseEntity<UrlResponse> stats =
                rest.getForEntity("/api/v1/urls/" + shortCode, UrlResponse.class);
        assertThat(stats.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stats.getBody()).isNotNull();
        assertThat(stats.getBody().hitCount()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidUrl() {
        ResponseEntity<ApiError> response =
                rest.postForEntity("/api/v1/urls", new CreateUrlRequest("not-a-valid-url"), ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
    }

    @Test
    void returnsNotFoundForUnknownCode() {
        ResponseEntity<ApiError> response =
                rest.getForEntity("/api/v1/urls/doesnotexist", ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void rootServesTheDashboard() {
        ResponseEntity<String> response = rest.getForEntity("/", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isNotNull()
                .satisfies(ct -> assertThat(ct.toString()).contains("text/html"));
        assertThat(response.getBody())
                .contains("URL Shortener")
                .contains("id=\"create-form\"");
    }

    @Test
    void exposesPrometheusMetrics() {
        ResponseEntity<String> response =
                rest.getForEntity("/actuator/prometheus", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("application_started_time_seconds");
    }
}
