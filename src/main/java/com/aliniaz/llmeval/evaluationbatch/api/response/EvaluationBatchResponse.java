package com.aliniaz.llmeval.evaluationbatch.api.response;

import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchStatus;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record EvaluationBatchResponse(
        Long id,
        Long workflowId,
        Long promptVersionId,
        String modelName,
        String modelVersion,
        EvaluationRunProvider provider,
        BigDecimal temperature,
        Map<String, Object> runConfig,
        EvaluationBatchStatus status,
        int totalRuns,
        int completedRuns,
        int passedRuns,
        int failedRuns,
        int erroredRuns,
        BigDecimal averageScore,
        List<String> failureReasons,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelRequestedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}