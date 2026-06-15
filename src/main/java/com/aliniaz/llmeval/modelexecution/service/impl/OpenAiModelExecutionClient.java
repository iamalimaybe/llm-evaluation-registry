package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionClient;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionRequest;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiModelExecutionClient implements ModelExecutionClient {

    private final RestTemplateBuilder restTemplateBuilder;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.base-url:https://api.openai.com}")
    private String openAiBaseUrl;

    @Override
    public boolean supports(EvaluationRunProvider provider) {
        return EvaluationRunProvider.OPENAI.equals(provider);
    }

    @Override
    public ModelExecutionResponse execute(ModelExecutionRequest request) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        RestTemplate restTemplate = restTemplateBuilder.build();

        OpenAiResponsesRequest openAiRequest = new OpenAiResponsesRequest(
                request.modelName(),
                request.prompt(),
                false,
                buildTextFormat(),
                request.temperature(),
                maxOutputTokens(request),
                topP(request)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiApiKey);

        HttpEntity<OpenAiResponsesRequest> entity = new HttpEntity<>(openAiRequest, headers);

        try {
            OpenAiResponsesResponse openAiResponse = restTemplate.postForObject(
                    openAiBaseUrl + "/v1/responses",
                    entity,
                    OpenAiResponsesResponse.class
            );

            if (openAiResponse == null) {
                throw new IllegalStateException("OpenAI returned an empty response.");
            }

            if (openAiResponse.error() != null) {
                throw new IllegalStateException(
                        "OpenAI returned an error: " + openAiResponse.error().message()
                );
            }

            if (openAiResponse.outputText() == null || openAiResponse.outputText().isBlank()) {
                throw new IllegalStateException("OpenAI returned no output text.");
            }

            return new ModelExecutionResponse(
                    openAiResponse.outputText(),
                    Map.of(
                            "provider", EvaluationRunProvider.OPENAI.name(),
                            "model", request.modelName(),
                            "format", "json_object",
                            "responseId", openAiResponse.id()
                    )
            );
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Failed to execute model through OpenAI. Status: %s. Response: %s".formatted(
                            exception.getStatusCode(),
                            exception.getResponseBodyAsString()
                    ),
                    exception
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to execute model through OpenAI.", exception);
        }
    }

    private Map<String, Object> buildTextFormat() {
        return Map.of(
                "format",
                Map.of("type", "json_object")
        );
    }

    private Integer maxOutputTokens(ModelExecutionRequest request) {
        Object value = runConfigValue(request, "numPredict");

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }

        return null;
    }

    private Number topP(ModelExecutionRequest request) {
        Object value = runConfigValue(request, "topP");

        if (value instanceof Number number) {
            return number;
        }

        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }

        return null;
    }

    private Object runConfigValue(ModelExecutionRequest request, String key) {
        if (request.runConfig() == null) {
            return null;
        }

        return request.runConfig().get(key);
    }

    private record OpenAiResponsesRequest(
            String model,
            String input,
            boolean store,
            Map<String, Object> text,
            java.math.BigDecimal temperature,
            Integer max_output_tokens,
            Number top_p
    ) {
    }

    private record OpenAiResponsesResponse(
            String id,
            String output_text,
            OpenAiError error
    ) {
        String outputText() {
            return output_text;
        }
    }

    private record OpenAiError(
            String message,
            String type,
            String code
    ) {
    }
}