package com.aliniaz.llmeval.evaluationbatch.service.impl;

import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationbatch.api.request.CreateEvaluationBatchRequest;
import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchResponse;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatch;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchRepository;
import com.aliniaz.llmeval.evaluationbatch.service.EvaluationBatchService;
import com.aliniaz.llmeval.evaluationbatch.service.EvaluationBatchWorker;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.prompt.service.PromptVersionService;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationBatchServiceImpl implements EvaluationBatchService {

    private final EvaluationBatchRepository evaluationBatchRepository;

    private final WorkflowService workflowService;
    private final PromptVersionService promptVersionService;
    private final EvaluationCaseService evaluationCaseService;
    private final EvaluationBatchWorker evaluationBatchWorker;

    @Override
    public EvaluationBatchResponse createEvaluationBatch(Long workflowId, CreateEvaluationBatchRequest request) {
        AiWorkflow workflow = workflowService.getWorkflowEntity(workflowId);

        PromptVersion promptVersion = promptVersionService.getPromptVersionEntity(
                workflowId,
                request.promptVersionId()
        );

        List<EvaluationCase> enabledEvaluationCases = evaluationCaseService.getEnabledEvaluationCaseEntities(workflowId);

        if (enabledEvaluationCases.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot create evaluation batch because workflow has no enabled evaluation cases."
            );
        }

        EvaluationRunProvider provider = request.provider() == null
                ? EvaluationRunProvider.OLLAMA
                : request.provider();

        EvaluationBatch evaluationBatch = new EvaluationBatch(
                workflow,
                promptVersion,
                request.modelName(),
                request.modelVersion(),
                provider,
                request.temperature(),
                request.runConfig(),
                enabledEvaluationCases.size()
        );

        EvaluationBatch savedEvaluationBatch = evaluationBatchRepository.save(evaluationBatch);
        triggerBatchWorkerAfterCommit();
        return toResponse(savedEvaluationBatch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvaluationBatchResponse> getEvaluationBatches(Long workflowId) {
        workflowService.getWorkflowEntity(workflowId);

        return evaluationBatchRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EvaluationBatchResponse getEvaluationBatch(Long workflowId, Long batchId) {
        return toResponse(getEvaluationBatchEntity(workflowId, batchId));
    }

    @Override
    public EvaluationBatchResponse cancelEvaluationBatch(Long workflowId, Long batchId) {
        EvaluationBatch evaluationBatch = getEvaluationBatchEntity(workflowId, batchId);

        evaluationBatch.requestCancel();

        return toResponse(evaluationBatch);
    }

    private EvaluationBatch getEvaluationBatchEntity(Long workflowId, Long batchId) {
        workflowService.getWorkflowEntity(workflowId);

        return evaluationBatchRepository.findByIdAndWorkflowId(batchId, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation batch not found with id: " + batchId
                ));
    }

    private EvaluationBatchResponse toResponse(EvaluationBatch evaluationBatch) {
        return new EvaluationBatchResponse(
                evaluationBatch.getId(),
                evaluationBatch.getWorkflow().getId(),
                evaluationBatch.getPromptVersion().getId(),
                evaluationBatch.getModelName(),
                evaluationBatch.getModelVersion(),
                evaluationBatch.getProvider(),
                evaluationBatch.getTemperature(),
                evaluationBatch.getRunConfig(),
                evaluationBatch.getStatus(),
                evaluationBatch.getTotalRuns(),
                evaluationBatch.getCompletedRuns(),
                evaluationBatch.getPassedRuns(),
                evaluationBatch.getFailedRuns(),
                evaluationBatch.getErroredRuns(),
                evaluationBatch.getAverageScore(),
                evaluationBatch.getFailureReasons(),
                evaluationBatch.getStartedAt(),
                evaluationBatch.getCompletedAt(),
                evaluationBatch.getCancelRequestedAt(),
                evaluationBatch.getCreatedAt(),
                evaluationBatch.getUpdatedAt()
        );
    }

    private void triggerBatchWorkerAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evaluationBatchWorker.processQueuedBatches();
                }
            });

            return;
        }

        evaluationBatchWorker.processQueuedBatches();
    }
}