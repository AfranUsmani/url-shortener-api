package io.github.afranusmani.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlShortenerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("URL Shortener API")
                .description("A production-grade URL shortener with cache-aside reads and Prometheus metrics.")
                .version("1.0.0")
                .contact(new Contact().name("Afran Usmani").url("https://github.com/AfranUsmani"))
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
