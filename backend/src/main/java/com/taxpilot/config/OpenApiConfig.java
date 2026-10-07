package com.taxpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taxPilotOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TaxPilot API")
                        .description("AI-powered Kenyan SME tax compliance platform")
                        .version("1.0.0"));
    }
}
