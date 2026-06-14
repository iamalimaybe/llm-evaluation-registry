package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptExecutionBuilderImplTest {

    private final PromptExecutionBuilderImpl promptExecutionBuilder = new PromptExecutionBuilderImpl(
            new ObjectMapper()
    );

    @Test
    void shouldBuildControlledPromptUsingWorkflowInstructionAndInputPayloadOnly() {
        AiWorkflow workflow = new AiWorkflow(
                "Shipping Status Evaluation",
                "Evaluates shipping status extraction"
        );

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Extract supported facts only and return a status field.",
                "Initial extraction prompt"
        );

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Missing shipping status",
                "EXTRACTION",
                Map.of("message", "The customer asked about shipping status."),
                Map.of("status", "SECRET_EXPECTED_ANSWER"),
                List.of("Shipping status is not specified"),
                List.of("shipped", "delivered"),
                null
        );

        EvaluationRun evaluationRun = new EvaluationRun(
                workflow,
                promptVersion,
                evaluationCase,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                BigDecimal.ZERO,
                Map.of("contextWindow", 4096)
        );

        String prompt = promptExecutionBuilder.buildPrompt(evaluationRun);

        assertThat(prompt).contains("Return valid JSON only.");
        assertThat(prompt).contains("Do not include reasoning.");
        assertThat(prompt).contains("Do not invent unsupported facts.");
        assertThat(prompt).contains("Workflow instruction:");
        assertThat(prompt).contains("Extract supported facts only and return a status field.");
        assertThat(prompt).contains("Evaluation input:");
        assertThat(prompt).contains("The customer asked about shipping status.");

        assertThat(prompt).doesNotContain("SECRET_EXPECTED_ANSWER");
        assertThat(prompt).doesNotContain("shipped");
        assertThat(prompt).doesNotContain("delivered");
    }
}