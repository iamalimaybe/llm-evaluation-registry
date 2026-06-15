package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionRequest;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaModelExecutionClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private OllamaModelExecutionClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        client = new OllamaModelExecutionClient(restTemplateBuilder);

        ReflectionTestUtils.setField(client, "ollamaBaseUrl", "http://ollama.test");

        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void supportsReturnsTrueOnlyForOllamaProvider() {
        assertThat(client.supports(EvaluationRunProvider.OLLAMA)).isTrue();
        assertThat(client.supports(EvaluationRunProvider.OPENAI)).isFalse();
        assertThat(client.supports(EvaluationRunProvider.ANTHROPIC)).isFalse();
        assertThat(client.supports(EvaluationRunProvider.MANUAL)).isFalse();
    }

    @Test
    void executeSendsGenerateRequestAndReturnsResponseText() {
        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.OLLAMA,
                "qwen3:4b",
                "Return JSON only.",
                BigDecimal.ZERO,
                Map.of(
                        "numPredict", 128,
                        "contextWindow", 4096,
                        "topP", 0.9,
                        "topK", 40,
                        "seed", 123
                )
        );

        server.expect(requestTo("http://ollama.test/api/generate"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.model", is("qwen3:4b")))
                .andExpect(jsonPath("$.prompt", is("Return JSON only.")))
                .andExpect(jsonPath("$.stream", is(false)))
                .andExpect(jsonPath("$.think", is(false)))
                .andExpect(jsonPath("$.format", is("json")))
                .andExpect(jsonPath("$.options.temperature", is(0)))
                .andExpect(jsonPath("$.options.num_predict", is(128)))
                .andExpect(jsonPath("$.options.num_ctx", is(4096)))
                .andExpect(jsonPath("$.options.top_p", is(0.9)))
                .andExpect(jsonPath("$.options.top_k", is(40)))
                .andExpect(jsonPath("$.options.seed", is(123)))
                .andRespond(withSuccess(
                        """
                        {
                          "response": "{\\"status\\":\\"INSUFFICIENT_INFORMATION\\"}"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        ModelExecutionResponse response = client.execute(request);

        assertThat(response.rawOutput()).isEqualTo("{\"status\":\"INSUFFICIENT_INFORMATION\"}");
        assertThat(response.metadata()).containsEntry("provider", "OLLAMA");
        assertThat(response.metadata()).containsEntry("model", "qwen3:4b");
        assertThat(response.metadata()).containsEntry("format", "json");

        server.verify();
    }

    @Test
    void executeFailsWhenOllamaReturnsThinkingButNoFinalResponse() {
        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.OLLAMA,
                "qwen3:4b",
                "Return JSON only.",
                BigDecimal.ZERO,
                Map.of("numPredict", 128)
        );

        server.expect(requestTo("http://ollama.test/api/generate"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                        {
                          "response": "",
                          "thinking": "I should think through the answer."
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ollama returned thinking text but no final response.");

        server.verify();
    }

    @Test
    void executeFailsWhenOllamaReturnsNoGeneratedText() {
        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.OLLAMA,
                "qwen3:4b",
                "Return JSON only.",
                BigDecimal.ZERO,
                Map.of("numPredict", 128)
        );

        server.expect(requestTo("http://ollama.test/api/generate"))
                .andExpect(method(POST))
                .andRespond(withSuccess(
                        """
                        {
                          "response": ""
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> client.execute(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Ollama returned no generated text.");

        server.verify();
    }
}