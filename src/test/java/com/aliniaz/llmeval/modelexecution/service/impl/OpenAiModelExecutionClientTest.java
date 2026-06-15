package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionRequest;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class OpenAiModelExecutionClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private OpenAiModelExecutionClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        client = new OpenAiModelExecutionClient(restTemplateBuilder);

        ReflectionTestUtils.setField(client, "openAiApiKey", "test-api-key");
        ReflectionTestUtils.setField(client, "openAiBaseUrl", "https://api.openai.test");

        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void supportsReturnsTrueOnlyForOpenAiProvider() {
        assertThat(client.supports(EvaluationRunProvider.OPENAI)).isTrue();
        assertThat(client.supports(EvaluationRunProvider.OLLAMA)).isFalse();
        assertThat(client.supports(EvaluationRunProvider.ANTHROPIC)).isFalse();
        assertThat(client.supports(EvaluationRunProvider.MANUAL)).isFalse();
    }

    @Test
    void executeSendsResponsesApiRequestAndReturnsOutputText() {
        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.OPENAI,
                "gpt-4.1-mini",
                "Return JSON only.",
                BigDecimal.ZERO,
                Map.of(
                        "numPredict", 128,
                        "topP", 0.9,
                        "contextWindow", 4096
                )
        );

        server.expect(requestTo("https://api.openai.test/v1/responses"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.model", is("gpt-4.1-mini")))
                .andExpect(jsonPath("$.input", is("Return JSON only.")))
                .andExpect(jsonPath("$.store", is(false)))
                .andExpect(jsonPath("$.text.format.type", is("json_object")))
                .andExpect(jsonPath("$.temperature", is(0)))
                .andExpect(jsonPath("$.max_output_tokens", is(128)))
                .andExpect(jsonPath("$.top_p", is(0.9)))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "resp_test_123",
                          "output_text": "{\\"status\\":\\"INSUFFICIENT_INFORMATION\\"}"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ModelExecutionResponse response = client.execute(request);

        assertThat(response.rawOutput()).isEqualTo("{\"status\":\"INSUFFICIENT_INFORMATION\"}");
        assertThat(response.metadata()).containsEntry("provider", "OPENAI");
        assertThat(response.metadata()).containsEntry("model", "gpt-4.1-mini");
        assertThat(response.metadata()).containsEntry("format", "json_object");
        assertThat(response.metadata()).containsEntry("responseId", "resp_test_123");

        server.verify();
    }

    @Test
    void executeFailsWhenApiKeyIsMissing() {
        ReflectionTestUtils.setField(client, "openAiApiKey", "");

        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.OPENAI,
                "gpt-4.1-mini",
                "Return JSON only.",
                BigDecimal.ZERO,
                Map.of("numPredict", 128)
        );

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAI API key is not configured.");
    }

    @Test
    void executeFailsWhenOpenAiReturnsNoOutputText() {
        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.OPENAI,
                "gpt-4.1-mini",
                "Return JSON only.",
                BigDecimal.ZERO,
                Map.of("numPredict", 128)
        );

        server.expect(requestTo("https://api.openai.test/v1/responses"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "resp_test_123",
                          "output_text": ""
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("OpenAI returned no output text.");

        server.verify();
    }
}