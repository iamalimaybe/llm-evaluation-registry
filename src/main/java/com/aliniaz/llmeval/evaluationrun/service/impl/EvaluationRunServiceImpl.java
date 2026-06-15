package com.aliniaz.llmeval.evaluationrun.service.impl;

import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatch;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import com.aliniaz.llmeval.evaluationrun.api.request.CompleteEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.request.CreateEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunRepository;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationResult;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunEvaluator;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import com.aliniaz.llmeval.modelexecution.service.*;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.prompt.service.PromptVersionService;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationRunServiceImpl implements EvaluationRunService {

    private final EvaluationRunRepository evaluationRunRepository;

    private final WorkflowService workflowService;
    private final PromptVersionService promptVersionService;
    private final EvaluationCaseService evaluationCaseService;

    private final ModelExecutionClientRouter modelExecutionClientRouter;
    private final PromptExecutionBuilder promptExecutionBuilder;
    private final ModelOutputParser modelOutputParser;
    private final EvaluationRunEvaluator evaluationRunEvaluator;

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

    @Override
    public EvaluationRunResponse completeEvaluationRun(
            Long workflowId,
            Long evaluationRunId,
            CompleteEvaluationRunRequest request
    ) {
        workflowService.getWorkflowEntity(workflowId);

        EvaluationRun evaluationRun = evaluationRunRepository.findByIdAndWorkflowId(evaluationRunId, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation run not found with id: " + evaluationRunId
                ));

        evaluationRun.complete(
                request.rawOutput(),
                request.parsedOutput(),
                request.confidence(),
                request.passed(),
                request.score(),
                request.failureReasons(),
                request.reviewerNotes()
        );

        return toResponse(evaluationRun);
    }

    @Override
    public EvaluationRunResponse executeEvaluationRun(Long workflowId, Long evaluationRunId) {
        workflowService.getWorkflowEntity(workflowId);

        EvaluationRun evaluationRun = evaluationRunRepository.findByIdAndWorkflowId(evaluationRunId, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation run not found with id: " + evaluationRunId
                ));

        evaluationRun.markRunning();

        try {
            ModelExecutionRequest executionRequest = new ModelExecutionRequest(
                    evaluationRun.getProvider(),
                    evaluationRun.getModelName(),
                    promptExecutionBuilder.buildPrompt(evaluationRun),
                    evaluationRun.getTemperature(),
                    evaluationRun.getRunConfig()
            );

            ModelExecutionClient client = modelExecutionClientRouter.getClient(executionRequest);
            ModelExecutionResponse executionResponse = client.execute(executionRequest);

            evaluationRun.recordModelOutput(executionResponse.rawOutput());
            evaluationRun.recordParsedOutput(modelOutputParser.parse(executionResponse.rawOutput()));

            EvaluationResult evaluationResult = evaluationRunEvaluator.evaluate(evaluationRun);

            evaluationRun.recordEvaluationResult(
                    evaluationResult.passed(),
                    evaluationResult.score(),
                    evaluationResult.failureReasons()
            );
        } catch (RuntimeException exception) {
            evaluationRun.recordExecutionError(exception.getMessage());
        }

        return toResponse(evaluationRun);
    }

    @Override
    public EvaluationRunResponse createAndExecuteBatchRun(
            Long workflowId,
            Long promptVersionId,
            Long evaluationCaseId,
            Long batchId,
            String modelName,
            String modelVersion,
            EvaluationRunProvider provider,
            BigDecimal temperature,
            Map<String, Object> runConfig
    ) {
        AiWorkflow workflow = workflowService.getWorkflowEntity(workflowId);

        PromptVersion promptVersion = promptVersionService.getPromptVersionEntity(
                workflowId,
                promptVersionId
        );

        EvaluationCase evaluationCase = evaluationCaseService.getEvaluationCaseEntity(
                workflowId,
                evaluationCaseId
        );

        EvaluationRun evaluationRun = new EvaluationRun(
                workflow,
                promptVersion,
                evaluationCase,
                modelName,
                modelVersion,
                provider,
                temperature,
                runConfig
        );

        evaluationRun.assignBatchId(batchId);

        EvaluationRun savedEvaluationRun = evaluationRunRepository.save(evaluationRun);

        return executeEvaluationRun(
                workflowId,
                savedEvaluationRun.getId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationRunResponse> getEvaluationRunsByBatch(Long workflowId, Long batchId) {
        workflowService.getWorkflowEntity(workflowId);

        return evaluationRunRepository.findByBatchIdOrderByCreatedAtAsc(batchId)
                .stream()
                .filter(evaluationRun -> evaluationRun.getWorkflow().getId().equals(workflowId))
                .map(this::toResponse)
                .toList();
    }
}