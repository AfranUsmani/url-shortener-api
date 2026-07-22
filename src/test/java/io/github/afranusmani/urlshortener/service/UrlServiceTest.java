package io.github.afranusmani.urlshortener.service;

import io.github.afranusmani.urlshortener.exception.ShortCodeNotFoundException;
import io.github.afranusmani.urlshortener.model.UrlMapping;
import io.github.afranusmani.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository repository;

    private UrlService service;

    @BeforeEach
    void setUp() {
        service = new UrlService(repository);
    }

    @Test
    void createPersistsAndReturnsMapping() {
        UrlMapping stored = new UrlMapping("https://example.com");
        stored.setShortCode("1C");
        when(repository.save(any(UrlMapping.class))).thenReturn(stored);

        UrlMapping result = service.create("https://example.com");

        assertThat(result.getShortCode()).isEqualTo("1C");
        assertThat(result.getOriginalUrl()).isEqualTo("https://example.com");
    }

    @Test
    void resolveReturnsOriginalUrl() {
        UrlMapping mapping = new UrlMapping("https://example.com/page");
        mapping.setShortCode("1C");
        when(repository.findByShortCode("1C")).thenReturn(Optional.of(mapping));

        assertThat(service.resolve("1C")).isEqualTo("https://example.com/page");
    }

    @Test
    void resolveThrowsWhenShortCodeMissing() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("missing"))
                .isInstanceOf(ShortCodeNotFoundException.class);
    }

    @Test
    void recordHitDelegatesToRepository() {
        service.recordHit("1C");
        verify(repository).incrementHitCount("1C");
    }
}
