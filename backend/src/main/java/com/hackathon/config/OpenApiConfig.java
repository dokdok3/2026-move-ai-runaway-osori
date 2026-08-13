package com.hackathon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI hackathonOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Hackathon API")
                .description("Logistics matching hackathon API")
                .version("v1"));
    }
}
