package com.aliniaz.llmeval.evaluationcase.service.impl;

import com.aliniaz.llmeval.common.exception.DuplicateResourceException;
import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationcase.api.request.CreateEvaluationCaseRequest;
import com.aliniaz.llmeval.evaluationcase.api.response.EvaluationCaseResponse;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCaseRepository;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationCaseServiceImpl implements EvaluationCaseService {

    private final EvaluationCaseRepository evaluationCaseRepository;
    private final WorkflowService workflowService;

    @Override
    public EvaluationCaseResponse createEvaluationCase(Long workflowId, CreateEvaluationCaseRequest request) {
        AiWorkflow workflow = workflowService.getWorkflowEntity(workflowId);

        if (evaluationCaseRepository.existsByWorkflowIdAndNameIgnoreCase(workflowId, request.name())) {
            throw new DuplicateResourceException(
                    "Evaluation case already exists for workflow id " + workflowId + ": " + request.name()
            );
        }

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                request.name(),
                request.taskType(),
                request.inputPayload(),
                request.expectedOutput(),
                request.requiredFacts(),
                request.forbiddenClaims(),
                request.scoringRules()
        );

        EvaluationCase savedEvaluationCase = evaluationCaseRepository.save(evaluationCase);

        return toResponse(savedEvaluationCase);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationCaseResponse> getEvaluationCases(Long workflowId) {
        workflowService.getWorkflowEntity(workflowId);

        return evaluationCaseRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationCaseResponse getEvaluationCase(Long workflowId, Long evaluationCaseId) {
        workflowService.getWorkflowEntity(workflowId);

        return evaluationCaseRepository.findByIdAndWorkflowId(evaluationCaseId, workflowId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation case not found with id: " + evaluationCaseId
                ));
    }

    private EvaluationCaseResponse toResponse(EvaluationCase evaluationCase) {
        return new EvaluationCaseResponse(
                evaluationCase.getId(),
                evaluationCase.getWorkflow().getId(),
                evaluationCase.getName(),
                evaluationCase.getTaskType(),
                evaluationCase.getInputPayload(),
                evaluationCase.getExpectedOutput(),
                evaluationCase.getRequiredFacts(),
                evaluationCase.getForbiddenClaims(),
                evaluationCase.getScoringRules(),
                evaluationCase.isEnabled(),
                evaluationCase.getCreatedAt(),
                evaluationCase.getUpdatedAt()
        );
    }
}