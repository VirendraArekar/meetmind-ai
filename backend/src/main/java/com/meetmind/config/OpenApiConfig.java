package com.meetmind.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI configuration for MeetMind AI REST APIs.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI meetMindOpenApi(@Value("${app.version:0.1.0}") String appVersion) {
        return new OpenAPI()
                .info(new Info()
                        .title("MeetMind AI API")
                        .description("Production-grade AI Meeting Assistant REST API")
                        .version(appVersion)
                        .contact(new Contact().name("MeetMind AI Engineering"))
                        .license(new License().name("MIT")));
    }
}
