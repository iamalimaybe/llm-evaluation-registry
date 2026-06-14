package com.aliniaz.llmeval.evaluationrun.service.impl;

import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import com.aliniaz.llmeval.evaluationrun.api.request.CreateEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunRepository;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.prompt.service.PromptVersionService;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationRunServiceImpl implements EvaluationRunService {

    private final EvaluationRunRepository evaluationRunRepository;
    private final WorkflowService workflowService;
    private final PromptVersionService promptVersionService;
    private final EvaluationCaseService evaluationCaseService;

    @Override
    public EvaluationRunResponse createEvaluationRun(Long workflowId, CreateEvaluationRunRequest request) {
        AiWorkflow workflow = workflowService.getWorkflowEntity(workflowId);
        PromptVersion promptVersion = promptVersionService.getPromptVersionEntity(
                workflowId,
                request.promptVersionId()
        );
        EvaluationCase evaluationCase = evaluationCaseService.getEvaluationCaseEntity(
                workflowId,
                request.evaluationCaseId()
        );

        EvaluationRunProvider provider = request.provider() == null
                ? EvaluationRunProvider.OLLAMA
                : request.provider();

        EvaluationRun evaluationRun = new EvaluationRun(
                workflow,
                promptVersion,
                evaluationCase,
                request.modelName(),
                request.modelVersion(),
                provider,
                request.temperature(),
                request.runConfig()
        );

        EvaluationRun savedEvaluationRun = evaluationRunRepository.save(evaluationRun);

        return toResponse(savedEvaluationRun);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationRunResponse> getEvaluationRuns(Long workflowId) {
        workflowService.getWorkflowEntity(workflowId);

        return evaluationRunRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationRunResponse getEvaluationRun(Long workflowId, Long evaluationRunId) {
        workflowService.getWorkflowEntity(workflowId);

        return evaluationRunRepository.findByIdAndWorkflowId(evaluationRunId, workflowId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation run not found with id: " + evaluationRunId
                ));
    }

    private EvaluationRunResponse toResponse(EvaluationRun evaluationRun) {
        return new EvaluationRunResponse(
                evaluationRun.getId(),
                evaluationRun.getWorkflow().getId(),
                evaluationRun.getPromptVersion().getId(),
                evaluationRun.getEvaluationCase().getId(),
                evaluationRun.getModelName(),
                evaluationRun.getModelVersion(),
                evaluationRun.getProvider(),
                evaluationRun.getRawOutput(),
                evaluationRun.getParsedOutput(),
                evaluationRun.getConfidence(),
                evaluationRun.getTemperature(),
                evaluationRun.getRunConfig(),
                evaluationRun.getStatus(),
                evaluationRun.getPassed(),
                evaluationRun.getScore(),
                evaluationRun.getFailureReasons(),
                evaluationRun.getReviewerNotes(),
                evaluationRun.getStartedAt(),
                evaluationRun.getCompletedAt(),
                evaluationRun.getCreatedAt(),
                evaluationRun.getUpdatedAt()
        );
    }
}