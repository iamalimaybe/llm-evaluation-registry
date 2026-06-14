package com.aliniaz.llmeval.evaluationrun.api;

import com.aliniaz.llmeval.evaluationrun.api.request.CompleteEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.request.CreateEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EvaluationRunController {

    private final EvaluationRunService evaluationRunService;

    @PostMapping("/api/workflows/{workflowId}/evaluation-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationRunResponse createEvaluationRun(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateEvaluationRunRequest request
    ) {
        return evaluationRunService.createEvaluationRun(workflowId, request);
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-runs")
    public List<EvaluationRunResponse> getEvaluationRuns(@PathVariable Long workflowId) {
        return evaluationRunService.getEvaluationRuns(workflowId);
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-runs/{evaluationRunId}")
    public EvaluationRunResponse getEvaluationRun(
            @PathVariable Long workflowId,
            @PathVariable Long evaluationRunId
    ) {
        return evaluationRunService.getEvaluationRun(workflowId, evaluationRunId);
    }

    @PatchMapping("/api/workflows/{workflowId}/evaluation-runs/{evaluationRunId}/result")
    public EvaluationRunResponse completeEvaluationRun(
            @PathVariable Long workflowId,
            @PathVariable Long evaluationRunId,
            @Valid @RequestBody CompleteEvaluationRunRequest request
    ) {
        return evaluationRunService.completeEvaluationRun(workflowId, evaluationRunId, request);
    }

    @PostMapping("/api/workflows/{workflowId}/evaluation-runs/{evaluationRunId}/execute")
    public EvaluationRunResponse executeEvaluationRun(
            @PathVariable Long workflowId,
            @PathVariable Long evaluationRunId
    ) {
        return evaluationRunService.executeEvaluationRun(workflowId, evaluationRunId);
    }
}