package com.aliniaz.llmeval.evaluationcase.api;

import com.aliniaz.llmeval.evaluationcase.api.request.CreateEvaluationCaseRequest;
import com.aliniaz.llmeval.evaluationcase.api.response.EvaluationCaseResponse;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EvaluationCaseController {

    private final EvaluationCaseService evaluationCaseService;

    @PostMapping("/api/workflows/{workflowId}/evaluation-cases")
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationCaseResponse createEvaluationCase(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateEvaluationCaseRequest request
    ) {
        return evaluationCaseService.createEvaluationCase(workflowId, request);
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-cases")
    public List<EvaluationCaseResponse> getEvaluationCases(@PathVariable Long workflowId) {
        return evaluationCaseService.getEvaluationCases(workflowId);
    }

    @GetMapping("/api/workflows/{workflowId}/evaluation-cases/{evaluationCaseId}")
    public EvaluationCaseResponse getEvaluationCase(
            @PathVariable Long workflowId,
            @PathVariable Long evaluationCaseId
    ) {
        return evaluationCaseService.getEvaluationCase(workflowId, evaluationCaseId);
    }
}