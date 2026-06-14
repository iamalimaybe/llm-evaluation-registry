package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionClient;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionRequest;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.firstNonBlank;

@Service
@RequiredArgsConstructor
public class OllamaModelExecutionClient implements ModelExecutionClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Override
    public boolean supports(EvaluationRunProvider provider) {
        return EvaluationRunProvider.OLLAMA.equals(provider);
    }

    @Override
    public ModelExecutionResponse execute(ModelExecutionRequest request) {
        RestTemplate restTemplate = restTemplateBuilder.build();

        OllamaGenerateRequest ollamaRequest = new OllamaGenerateRequest(
                request.modelName(),
                request.prompt(),
                false,
                false,
                buildOptions(request)
        );

        try {
            OllamaGenerateResponse ollamaResponse = restTemplate.postForObject(
                    ollamaBaseUrl + "/api/generate",
                    ollamaRequest,
                    OllamaGenerateResponse.class
            );

            if (ollamaResponse == null) {
                throw new IllegalStateException("Ollama returned an empty response.");
            }

            if (ollamaResponse.response() == null || ollamaResponse.response().isBlank()) {
                if (ollamaResponse.thinking() != null && !ollamaResponse.thinking().isBlank()) {
                    throw new IllegalStateException("Ollama returned thinking text but no final response.");
                }

                throw new IllegalStateException("Ollama returned no generated text.");
            }

            return new ModelExecutionResponse(
                    ollamaResponse.response(),
                    Map.of(
                            "provider", EvaluationRunProvider.OLLAMA.name(),
                            "model", request.modelName()
                    )
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to execute model through Ollama.", exception);
        }
    }

    private Map<String, Object> buildOptions(ModelExecutionRequest request) {
        Map<String, Object> options = new HashMap<>();

        if (request.temperature() != null) {
            options.put("temperature", request.temperature());
        }

        if (request.runConfig() != null) {
            copyIfPresent(request.runConfig(), options, "numPredict", "num_predict");
            copyIfPresent(request.runConfig(), options, "contextWindow", "num_ctx");
            copyIfPresent(request.runConfig(), options, "topP", "top_p");
            copyIfPresent(request.runConfig(), options, "topK", "top_k");
            copyIfPresent(request.runConfig(), options, "seed", "seed");
        }

        return options;
    }

    private void copyIfPresent(
            Map<String, Object> source,
            Map<String, Object> target,
            String sourceKey,
            String targetKey
    ) {
        if (source.containsKey(sourceKey)) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    private record OllamaGenerateRequest(
            String model,
            String prompt,
            boolean stream,
            Boolean think,
            Map<String, Object> options
    ) {
    }

    private record OllamaGenerateResponse(
            String response,
            String thinking
    ) {
    }
}