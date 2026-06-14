package com.aliniaz.llmeval.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI llmEvaluationRegistryOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LLM Evaluation Registry API")
                        .version("0.1.0")
                        .description("""
                                Backend API for tracking AI workflows, prompt versions, evaluation cases,
                                evaluation runs, manual results, and regression comparisons.
                                """));
    }
}