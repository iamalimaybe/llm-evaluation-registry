package com.aliniaz.llmeval.regression.api;

import com.aliniaz.llmeval.regression.api.response.EvaluationRunComparisonResponse;
import com.aliniaz.llmeval.regression.service.RegressionComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RegressionComparisonController {

    private final RegressionComparisonService regressionComparisonService;

    @GetMapping("/api/workflows/{workflowId}/evaluation-runs/compare")
    public EvaluationRunComparisonResponse compareRuns(
            @PathVariable Long workflowId,
            @RequestParam Long baselineRunId,
            @RequestParam Long candidateRunId
    ) {
        return regressionComparisonService.compareRuns(
                workflowId,
                baselineRunId,
                candidateRunId
        );
    }
}