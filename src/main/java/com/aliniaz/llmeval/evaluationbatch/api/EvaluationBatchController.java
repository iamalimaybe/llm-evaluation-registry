package com.aliniaz.llmeval.evaluationbatch.api;

import com.aliniaz.llmeval.evaluationbatch.api.request.CreateEvaluationBatchRequest;
import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchComparisonResponse;
import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchResponse;
import com.aliniaz.llmeval.evaluationbatch.service.EvaluationBatchComparisonService;
import com.aliniaz.llmeval.evaluationbatch.service.EvaluationBatchService;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EvaluationBatchController {

    private final EvaluationBatchService evaluationBatchService;
    private final EvaluationBatchComparisonService evaluationBatchComparisonService;
    private final EvaluationRunService evaluationRunService;

    @PostMapping("/api/workflows/{workflowId}/evaluation-batches")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationBatchResponse createEvaluationBatch(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateEvaluationBatchRequest request
    ) {
        return evaluationBatchService.createEvaluationBatch(workflowId, request);
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-batches")
    public List<EvaluationBatchResponse> getEvaluationBatches(@PathVariable Long workflowId) {
        return evaluationBatchService.getEvaluationBatches(workflowId);
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-batches/compare")
    public EvaluationBatchComparisonResponse compareEvaluationBatches(
            @PathVariable Long workflowId,
            @RequestParam Long baselineBatchId,
            @RequestParam Long candidateBatchId
    ) {
        return evaluationBatchComparisonService.compareEvaluationBatches(
                workflowId,
                baselineBatchId,
                candidateBatchId
        );
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-batches/{batchId}")
    public EvaluationBatchResponse getEvaluationBatch(
            @PathVariable Long workflowId,
            @PathVariable Long batchId
    ) {
        return evaluationBatchService.getEvaluationBatch(workflowId, batchId);
    }

    @PostMapping("/api/workflows/{workflowId}/evaluation-batches/{batchId}/cancel")
    public EvaluationBatchResponse cancelEvaluationBatch(
            @PathVariable Long workflowId,
            @PathVariable Long batchId
    ) {
        return evaluationBatchService.cancelEvaluationBatch(workflowId, batchId);
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-batches/{batchId}/runs")
    public List<EvaluationRunResponse> getEvaluationBatchRuns(
            @PathVariable Long workflowId,
            @PathVariable Long batchId
    ) {
        return evaluationRunService.getEvaluationRunsByBatch(workflowId, batchId);
    }
}