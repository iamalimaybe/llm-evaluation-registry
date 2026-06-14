package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;
import com.aliniaz.llmeval.modelexecution.service.PromptExecutionBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PromptExecutionBuilderImpl implements PromptExecutionBuilder {

    private final ObjectMapper objectMapper;

    @Override
    public String buildPrompt(EvaluationRun evaluationRun) {
        try {
            String inputPayloadJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(evaluationRun.getEvaluationCase().getInputPayload());

            return """
                    You are executing an evaluation case for an LLM workflow.

                    Rules:
                    Return valid JSON only.
                    Do not include markdown.
                    Do not include explanations outside JSON.
                    Do not include reasoning.
                    Do not include thinking text.
                    Use only the provided evaluation input.
                    Do not invent unsupported facts.
                    If the evaluation input does not contain enough information, represent that honestly in the JSON output.

                    Workflow instruction:
                    %s

                    Evaluation input:
                    %s

                    Return the JSON output requested by the workflow instruction.
                    """.formatted(
                    evaluationRun.getPromptVersion().getPromptText(),
                    inputPayloadJson
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to build model execution prompt.", exception);
        }
    }
}