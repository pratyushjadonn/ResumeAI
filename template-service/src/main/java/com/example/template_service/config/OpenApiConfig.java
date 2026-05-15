package com.example.template_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI templateServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ResumeAI Template Service API")
                        .description("OpenAPI documentation for ResumeAI template APIs.")
                        .version("1.0"));
    }
}
