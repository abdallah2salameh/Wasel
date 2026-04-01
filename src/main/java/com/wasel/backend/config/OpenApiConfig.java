package com.wasel.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI waselOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Wasel Palestine API")
                .version("v1")
                .description("Versioned backend APIs for incidents, checkpoints, routing, reports, and alerts.")
                .contact(new Contact().name("Wasel Backend Team"))
                .license(new License().name("Internal Academic Use")));
    }
}
