package com.aliniaz.llmeval.evaluationbatch.service.impl;

import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatch;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchRepository;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchStatus;
import com.aliniaz.llmeval.evaluationbatch.service.EvaluationBatchWorker;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunStatus;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class EvaluationBatchWorkerImpl implements EvaluationBatchWorker {

    private static final List<EvaluationBatchStatus> ACTIVE_BATCH_STATUSES = List.of(
            EvaluationBatchStatus.RUNNING,
            EvaluationBatchStatus.CANCEL_REQUESTED
    );

    private final ReentrantLock processingLock = new ReentrantLock();

    private final EvaluationBatchRepository evaluationBatchRepository;
    private final EvaluationCaseService evaluationCaseService;
    private final EvaluationRunService evaluationRunService;
    private final TransactionTemplate transactionTemplate;

    @Async("evaluationBatchExecutor")
    @Override
    public void processQueuedBatches() {
        if (!processingLock.tryLock()) {
            return;
        }

        try {
            while (true) {
                BatchPointer batchPointer = startNextQueuedBatch();

                if (batchPointer == null) {
                    return;
                }

                processBatch(batchPointer.workflowId(), batchPointer.batchId());
            }
        } finally {
            processingLock.unlock();
        }
    }

    private BatchPointer startNextQueuedBatch() {
        return transactionTemplate.execute(status -> {
            if (evaluationBatchRepository.existsByStatusIn(ACTIVE_BATCH_STATUSES)) {
                return null;
            }

            Optional<EvaluationBatch> queuedBatch = evaluationBatchRepository
                    .findFirstByStatusOrderByCreatedAtAsc(EvaluationBatchStatus.QUEUED);

            if (queuedBatch.isEmpty()) {
                return null;
            }

            EvaluationBatch batch = queuedBatch.get();

            batch.markRunning();

            return new BatchPointer(
                    batch.getWorkflow().getId(),
                    batch.getId()
            );
        });
    }

    private void processBatch(Long workflowId, Long batchId) {
        List<EvaluationCase> evaluationCases = evaluationCaseService.getEnabledEvaluationCaseEntities(workflowId);

        int completedRuns = 0;
        int passedRuns = 0;
        int failedRuns = 0;
        int erroredRuns = 0;
        BigDecimal scoreTotal = BigDecimal.ZERO;

        try {
            for (EvaluationCase evaluationCase : evaluationCases) {
                if (isCancellationRequested(batchId)) {
                    markCancelled(batchId, completedRuns, passedRuns, failedRuns, erroredRuns, scoreTotal);
                    return;
                }

                BatchExecutionContext batch = getBatchExecutionContext(batchId);

                EvaluationRunResponse runResponse = evaluationRunService.createAndExecuteBatchRun(
                        batch.workflowId(),
                        batch.promptVersionId(),
                        evaluationCase.getId(),
                        batch.batchId(),
                        batch.modelName(),
                        batch.modelVersion(),
                        batch.provider(),
                        batch.temperature(),
                        batch.runConfig()
                );

                completedRuns++;

                if (runResponse.status() == EvaluationRunStatus.ERROR) {
                    erroredRuns++;
                } else if (Boolean.TRUE.equals(runResponse.passed())) {
                    passedRuns++;
                } else {
                    failedRuns++;
                }

                if (runResponse.score() != null) {
                    scoreTotal = scoreTotal.add(runResponse.score());
                }

                recordProgress(batchId, completedRuns, passedRuns, failedRuns, erroredRuns, scoreTotal);
            }

            markCompleted(batchId);
        } catch (RuntimeException exception) {
            markError(batchId, exception.getMessage());
        }
    }

    private BatchExecutionContext getBatchExecutionContext(Long batchId) {
        return transactionTemplate.execute(status -> {
            EvaluationBatch batch = evaluationBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Evaluation batch not found with id: " + batchId));

            return new BatchExecutionContext(
                    batch.getWorkflow().getId(),
                    batch.getPromptVersion().getId(),
                    batch.getId(),
                    batch.getModelName(),
                    batch.getModelVersion(),
                    batch.getProvider(),
                    batch.getTemperature(),
                    batch.getRunConfig()
            );
        });
    }

    private boolean isCancellationRequested(Long batchId) {
        return transactionTemplate.execute(status -> evaluationBatchRepository.findById(batchId)
                .map(EvaluationBatch::isCancellationRequested)
                .orElse(false));
    }

    private void recordProgress(
            Long batchId,
            int completedRuns,
            int passedRuns,
            int failedRuns,
            int erroredRuns,
            BigDecimal scoreTotal
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            EvaluationBatch batch = evaluationBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Evaluation batch not found with id: " + batchId));

            batch.recordProgress(
                    completedRuns,
                    passedRuns,
                    failedRuns,
                    erroredRuns,
                    averageScore(scoreTotal, completedRuns)
            );
        });
    }

    private void markCompleted(Long batchId) {
        transactionTemplate.executeWithoutResult(status -> {
            EvaluationBatch batch = evaluationBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Evaluation batch not found with id: " + batchId));

            batch.markCompleted();
        });
    }

    private void markCancelled(
            Long batchId,
            int completedRuns,
            int passedRuns,
            int failedRuns,
            int erroredRuns,
            BigDecimal scoreTotal
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            EvaluationBatch batch = evaluationBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Evaluation batch not found with id: " + batchId));

            batch.recordProgress(
                    completedRuns,
                    passedRuns,
                    failedRuns,
                    erroredRuns,
                    averageScore(scoreTotal, completedRuns)
            );

            batch.markCancelled();
        });
    }

    private void markError(Long batchId, String failureReason) {
        transactionTemplate.executeWithoutResult(status -> {
            EvaluationBatch batch = evaluationBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalStateException("Evaluation batch not found with id: " + batchId));

            batch.markError(failureReason);
        });
    }

    private BigDecimal averageScore(BigDecimal scoreTotal, int completedRuns) {
        if (completedRuns == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return scoreTotal.divide(
                BigDecimal.valueOf(completedRuns),
                2,
                RoundingMode.HALF_UP
        );
    }

    private record BatchPointer(
            Long workflowId,
            Long batchId
    ) {
    }

    private record BatchExecutionContext(
            Long workflowId,
            Long promptVersionId,
            Long batchId,
            String modelName,
            String modelVersion,
            EvaluationRunProvider provider,
            BigDecimal temperature,
            java.util.Map<String, Object> runConfig
    ) {
    }
}