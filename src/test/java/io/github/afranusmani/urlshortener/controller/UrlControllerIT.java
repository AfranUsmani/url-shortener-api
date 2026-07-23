package io.github.afranusmani.urlshortener.controller;

import io.github.afranusmani.urlshortener.dto.AnalyticsResponse;
import io.github.afranusmani.urlshortener.dto.CreateUrlRequest;
import io.github.afranusmani.urlshortener.dto.UrlResponse;
import io.github.afranusmani.urlshortener.exception.ApiError;
import io.github.afranusmani.urlshortener.model.UrlMapping;
import io.github.afranusmani.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

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

    @Autowired
    private UrlRepository repository;

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

    @Test
    void createsAndResolvesACustomAlias() {
        String alias = "spring-boot-" + (System.nanoTime() % 1_000_000);
        ResponseEntity<UrlResponse> created = rest.postForEntity(
                "/api/v1/urls",
                new CreateUrlRequest("https://spring.io", alias, null),
                UrlResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        assertThat(created.getBody().shortCode()).isEqualTo(alias);

        ResponseEntity<Void> redirect = rest.getForEntity("/" + alias, Void.class);
        assertThat(redirect.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(redirect.getHeaders().getLocation()).isEqualTo(URI.create("https://spring.io"));
    }

    @Test
    void rejectsDuplicateAliasWithConflict() {
        String alias = "dup-" + (System.nanoTime() % 1_000_000);
        rest.postForEntity("/api/v1/urls",
                new CreateUrlRequest("https://spring.io", alias, null), UrlResponse.class);

        ResponseEntity<ApiError> second = rest.postForEntity("/api/v1/urls",
                new CreateUrlRequest("https://example.com", alias, null), ApiError.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).isNotNull();
        assertThat(second.getBody().status()).isEqualTo(409);
    }

    @Test
    void rejectsPastExpiryOnCreate() {
        ResponseEntity<ApiError> response = rest.postForEntity(
                "/api/v1/urls",
                new CreateUrlRequest("https://spring.io", null, Instant.now().minusSeconds(60)),
                ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void expiredLinkReturnsGone() {
        // Persist directly with a past expiry (the API rejects past expiry on create).
        UrlMapping mapping = repository.save(
                new UrlMapping("https://spring.io", Instant.now().minusSeconds(60)));

        ResponseEntity<ApiError> response =
                rest.getForEntity("/" + mapping.getShortCode(), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(410);
    }

    @Test
    void servesAQrCodePng() {
        ResponseEntity<UrlResponse> created = rest.postForEntity(
                "/api/v1/urls", new CreateUrlRequest("https://spring.io"), UrlResponse.class);
        String code = created.getBody().shortCode();

        ResponseEntity<byte[]> qr = rest.getForEntity("/api/v1/urls/" + code + "/qr", byte[].class);

        assertThat(qr.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(qr.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        byte[] body = qr.getBody();
        assertThat(body).isNotEmpty();
        // PNG magic number: 0x89 'P' 'N' 'G'
        assertThat(body[0] & 0xFF).isEqualTo(0x89);
        assertThat(new String(body, 1, 3)).isEqualTo("PNG");
    }

    @Test
    void aggregatesClickAnalyticsAsynchronously() {
        ResponseEntity<UrlResponse> created = rest.postForEntity(
                "/api/v1/urls", new CreateUrlRequest("https://spring.io"), UrlResponse.class);
        String code = created.getBody().shortCode();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605 Version/17.0 Mobile Safari/604");
        headers.set(HttpHeaders.REFERER, "https://twitter.com/some/post");
        rest.exchange("/" + code, HttpMethod.GET, new HttpEntity<>(headers), Void.class);
        rest.exchange("/" + code, HttpMethod.GET, new HttpEntity<>(headers), Void.class);

        // Click capture is async; wait for the events to land, then assert the breakdowns.
        await().atMost(10, SECONDS).untilAsserted(() -> {
            ResponseEntity<AnalyticsResponse> analytics =
                    rest.getForEntity("/api/v1/urls/" + code + "/analytics", AnalyticsResponse.class);
            assertThat(analytics.getStatusCode()).isEqualTo(HttpStatus.OK);
            AnalyticsResponse body = analytics.getBody();
            assertThat(body).isNotNull();
            assertThat(body.totalClicks()).isGreaterThanOrEqualTo(2);
            assertThat(body.byDevice()).containsKey("Mobile");
            assertThat(body.byBrowser()).containsKey("Safari");
            assertThat(body.byReferrer()).containsKey("twitter.com");
            assertThat(body.clicksByDay()).isNotEmpty();
        });
    }

    @Test
    void analyticsForUnknownCodeReturnsNotFound() {
        ResponseEntity<ApiError> response =
                rest.getForEntity("/api/v1/urls/nope" + System.nanoTime() + "/analytics", ApiError.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
