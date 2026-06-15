package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionClient;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ModelExecutionClientRouterImplTest {

    @Test
    void getClientReturnsClientThatSupportsRequestedProvider() {
        ModelExecutionClient ollamaClient = mock(ModelExecutionClient.class);
        ModelExecutionClient openAiClient = mock(ModelExecutionClient.class);

        when(ollamaClient.supports(EvaluationRunProvider.OPENAI)).thenReturn(false);
        when(openAiClient.supports(EvaluationRunProvider.OPENAI)).thenReturn(true);

        ModelExecutionClientRouterImpl router = new ModelExecutionClientRouterImpl(
                List.of(ollamaClient, openAiClient)
        );

        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.OPENAI,
                "gpt-4.1-mini",
                "Return JSON only.",
                null,
                null
        );

        assertThat(router.getClient(request)).isSameAs(openAiClient);
    }

    @Test
    void getClientThrowsWhenNoClientSupportsRequestedProvider() {
        ModelExecutionClient ollamaClient = mock(ModelExecutionClient.class);

        when(ollamaClient.supports(EvaluationRunProvider.ANTHROPIC)).thenReturn(false);

        ModelExecutionClientRouterImpl router = new ModelExecutionClientRouterImpl(
                List.of(ollamaClient)
        );

        ModelExecutionRequest request = new ModelExecutionRequest(
                EvaluationRunProvider.ANTHROPIC,
                "claude-placeholder",
                "Return JSON only.",
                null,
                null
        );

        assertThatThrownBy(() -> router.getClient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No model execution client configured for provider: ANTHROPIC");
    }
}