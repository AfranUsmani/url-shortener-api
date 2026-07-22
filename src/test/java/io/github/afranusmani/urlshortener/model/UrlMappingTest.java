package io.github.afranusmani.urlshortener.model;

import io.github.afranusmani.urlshortener.service.Base62;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class UrlMappingTest {

    @Test
    void assignsShortCodeDerivedFromId() {
        UrlMapping mapping = new UrlMapping("https://example.com");
        ReflectionTestUtils.setField(mapping, "id", 100L);

        mapping.assignShortCode();

        assertThat(mapping.getShortCode()).isEqualTo(Base62.encode(100L));
    }

    @Test
    void doesNotOverwriteAnExistingShortCode() {
        UrlMapping mapping = new UrlMapping("https://example.com");
        ReflectionTestUtils.setField(mapping, "id", 100L);
        mapping.setShortCode("custom");

        mapping.assignShortCode();

        assertThat(mapping.getShortCode()).isEqualTo("custom");
    }
}
