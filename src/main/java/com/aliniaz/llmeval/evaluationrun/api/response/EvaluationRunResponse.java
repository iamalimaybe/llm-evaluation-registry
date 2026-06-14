package com.aliniaz.llmeval.evaluationrun.api.response;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EvaluationRunResponse(
        Long id,
        Long workflowId,
        Long promptVersionId,
        Long evaluationCaseId,
        String modelName,
        String modelVersion,
        EvaluationRunProvider provider,
        String rawOutput,
        Map<String, Object> parsedOutput,
        BigDecimal confidence,
        BigDecimal temperature,
        Map<String, Object> runConfig,
        EvaluationRunStatus status,
        Boolean passed,
        BigDecimal score,
        List<String> failureReasons,
        String reviewerNotes,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}