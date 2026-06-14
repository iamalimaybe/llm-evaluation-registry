package com.aliniaz.llmeval.evaluationcase.api.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EvaluationCaseResponse(
        Long id,
        Long workflowId,
        String name,
        String taskType,
        Map<String, Object> inputPayload,
        Map<String, Object> expectedOutput,
        List<String> requiredFacts,
        List<String> forbiddenClaims,
        Map<String, Object> scoringRules,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}