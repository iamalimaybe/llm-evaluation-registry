package com.aliniaz.llmeval.evaluationbatch.service.impl;

import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchComparisonResponse;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatch;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchRepository;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchStatus;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunStatus;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import com.aliniaz.llmeval.regression.domain.RegressionComparisonOutcome;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationBatchComparisonServiceImplTest {

    @Mock
    private EvaluationBatchRepository evaluationBatchRepository;

    @Mock
    private EvaluationRunService evaluationRunService;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private EvaluationBatchComparisonServiceImpl comparisonService;

    @Test
    void compareEvaluationBatchesReturnsImprovedWhenCandidateImprovesFailedCase() {
        Long workflowId = 7L;
        Long baselineBatchId = 9L;
        Long candidateBatchId = 10L;

        mockWorkflow(workflowId);
        mockBatchLookup(
                workflowId,
                batch(baselineBatchId, EvaluationBatchStatus.COMPLETED_WITH_FAILURES, 2, 2, 1, 1, 0, "50.00"),
                batch(candidateBatchId, EvaluationBatchStatus.COMPLETED, 2, 2, 2, 0, 0, "100.00")
        );

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, baselineBatchId))
                .thenReturn(List.of(
                        run(42L, workflowId, 20L, EvaluationRunStatus.PASSED, true, "100.00"),
                        run(43L, workflowId, 21L, EvaluationRunStatus.FAILED, false, "0.00")
                ));

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, candidateBatchId))
                .thenReturn(List.of(
                        run(46L, workflowId, 20L, EvaluationRunStatus.PASSED, true, "100.00"),
                        run(47L, workflowId, 21L, EvaluationRunStatus.PASSED, true, "100.00")
                ));

        EvaluationBatchComparisonResponse response = comparisonService.compareEvaluationBatches(
                workflowId,
                baselineBatchId,
                candidateBatchId
        );

        assertThat(response.outcome()).isEqualTo(RegressionComparisonOutcome.IMPROVED);
        assertThat(response.scoreDelta()).isEqualByComparingTo("50.00");
        assertThat(response.comparisonReasons()).contains(
                "Candidate batch has fewer failed runs.",
                "Candidate batch has more passed runs.",
                "Candidate average score is higher than baseline average score.",
                "One or more evaluation cases improved."
        );

        assertThat(response.caseComparisons()).hasSize(2);
        assertThat(response.caseComparisons().get(0).outcome()).isEqualTo(RegressionComparisonOutcome.UNCHANGED);
        assertThat(response.caseComparisons().get(1).outcome()).isEqualTo(RegressionComparisonOutcome.IMPROVED);
    }

    @Test
    void compareEvaluationBatchesReturnsRegressedWhenCandidateIntroducesFailure() {
        Long workflowId = 7L;
        Long baselineBatchId = 9L;
        Long candidateBatchId = 10L;

        mockWorkflow(workflowId);
        mockBatchLookup(
                workflowId,
                batch(baselineBatchId, EvaluationBatchStatus.COMPLETED, 2, 2, 2, 0, 0, "100.00"),
                batch(candidateBatchId, EvaluationBatchStatus.COMPLETED_WITH_FAILURES, 2, 2, 1, 1, 0, "50.00")
        );

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, baselineBatchId))
                .thenReturn(List.of(
                        run(42L, workflowId, 20L, EvaluationRunStatus.PASSED, true, "100.00"),
                        run(43L, workflowId, 21L, EvaluationRunStatus.PASSED, true, "100.00")
                ));

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, candidateBatchId))
                .thenReturn(List.of(
                        run(46L, workflowId, 20L, EvaluationRunStatus.PASSED, true, "100.00"),
                        run(47L, workflowId, 21L, EvaluationRunStatus.FAILED, false, "0.00")
                ));

        EvaluationBatchComparisonResponse response = comparisonService.compareEvaluationBatches(
                workflowId,
                baselineBatchId,
                candidateBatchId
        );

        assertThat(response.outcome()).isEqualTo(RegressionComparisonOutcome.REGRESSED);
        assertThat(response.scoreDelta()).isEqualByComparingTo("-50.00");
        assertThat(response.comparisonReasons()).contains(
                "Candidate batch has more failed runs.",
                "Candidate batch has fewer passed runs.",
                "Candidate average score is lower than baseline average score.",
                "One or more evaluation cases regressed."
        );

        assertThat(response.caseComparisons()).hasSize(2);
        assertThat(response.caseComparisons().get(1).outcome()).isEqualTo(RegressionComparisonOutcome.REGRESSED);
        assertThat(response.caseComparisons().get(1).regressionReasons()).contains(
                "Candidate run failed while baseline run passed.",
                "Candidate score is lower than baseline score."
        );
    }

    @Test
    void compareEvaluationBatchesReturnsUnchangedWhenCandidateMatchesBaseline() {
        Long workflowId = 7L;
        Long baselineBatchId = 9L;
        Long candidateBatchId = 10L;

        mockWorkflow(workflowId);
        mockBatchLookup(
                workflowId,
                batch(baselineBatchId, EvaluationBatchStatus.COMPLETED_WITH_FAILURES, 2, 2, 1, 1, 0, "50.00"),
                batch(candidateBatchId, EvaluationBatchStatus.COMPLETED_WITH_FAILURES, 2, 2, 1, 1, 0, "50.00")
        );

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, baselineBatchId))
                .thenReturn(List.of(
                        run(42L, workflowId, 20L, EvaluationRunStatus.PASSED, true, "100.00"),
                        run(43L, workflowId, 21L, EvaluationRunStatus.FAILED, false, "0.00")
                ));

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, candidateBatchId))
                .thenReturn(List.of(
                        run(46L, workflowId, 20L, EvaluationRunStatus.PASSED, true, "100.00"),
                        run(47L, workflowId, 21L, EvaluationRunStatus.FAILED, false, "0.00")
                ));

        EvaluationBatchComparisonResponse response = comparisonService.compareEvaluationBatches(
                workflowId,
                baselineBatchId,
                candidateBatchId
        );

        assertThat(response.outcome()).isEqualTo(RegressionComparisonOutcome.UNCHANGED);
        assertThat(response.scoreDelta()).isEqualByComparingTo("0.00");
        assertThat(response.comparisonReasons()).containsExactly(
                "Candidate batch matched baseline score and run outcome counts."
        );
        assertThat(response.caseComparisons())
                .allMatch(caseComparison -> caseComparison.outcome() == RegressionComparisonOutcome.UNCHANGED);
    }

    @Test
    void compareEvaluationBatchesReturnsNotComparableWhenCandidateBatchIsCancelled() {
        Long workflowId = 7L;
        Long baselineBatchId = 9L;
        Long candidateBatchId = 10L;

        mockWorkflow(workflowId);
        mockBatchLookup(
                workflowId,
                batch(baselineBatchId, EvaluationBatchStatus.COMPLETED, 1, 1, 1, 0, 0, "100.00"),
                batch(candidateBatchId, EvaluationBatchStatus.CANCELLED, 1, 0, 0, 0, 0, "0.00")
        );

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, baselineBatchId))
                .thenReturn(List.of(
                        run(42L, workflowId, 20L, EvaluationRunStatus.PASSED, true, "100.00")
                ));

        when(evaluationRunService.getEvaluationRunsByBatch(workflowId, candidateBatchId))
                .thenReturn(List.of(
                        run(46L, workflowId, 20L, EvaluationRunStatus.PENDING, null, null)
                ));

        EvaluationBatchComparisonResponse response = comparisonService.compareEvaluationBatches(
                workflowId,
                baselineBatchId,
                candidateBatchId
        );

        assertThat(response.outcome()).isEqualTo(RegressionComparisonOutcome.NOT_COMPARABLE);
        assertThat(response.comparisonReasons()).contains("Candidate batch is not completed.");
    }

    private void mockWorkflow(Long workflowId) {
        when(workflowService.getWorkflowEntity(workflowId)).thenReturn(mock(AiWorkflow.class));
    }

    private void mockBatchLookup(
            Long workflowId,
            EvaluationBatch baselineBatch,
            EvaluationBatch candidateBatch
    ) {
        when(evaluationBatchRepository.findByIdAndWorkflowId(baselineBatch.getId(), workflowId))
                .thenReturn(Optional.of(baselineBatch));

        when(evaluationBatchRepository.findByIdAndWorkflowId(candidateBatch.getId(), workflowId))
                .thenReturn(Optional.of(candidateBatch));
    }

    private EvaluationBatch batch(
            Long id,
            EvaluationBatchStatus status,
            int totalRuns,
            int completedRuns,
            int passedRuns,
            int failedRuns,
            int erroredRuns,
            String averageScore
    ) {
        EvaluationBatch batch = mock(EvaluationBatch.class);

        when(batch.getId()).thenReturn(id);
        when(batch.getStatus()).thenReturn(status);
        when(batch.getTotalRuns()).thenReturn(totalRuns);
        when(batch.getCompletedRuns()).thenReturn(completedRuns);
        when(batch.getPassedRuns()).thenReturn(passedRuns);
        when(batch.getFailedRuns()).thenReturn(failedRuns);
        when(batch.getErroredRuns()).thenReturn(erroredRuns);
        when(batch.getAverageScore()).thenReturn(new BigDecimal(averageScore));

        return batch;
    }

    private EvaluationRunResponse run(
            Long id,
            Long workflowId,
            Long evaluationCaseId,
            EvaluationRunStatus status,
            Boolean passed,
            String score
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new EvaluationRunResponse(
                id,
                workflowId,
                2L,
                evaluationCaseId,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                null,
                null,
                null,
                BigDecimal.ZERO,
                Map.of("contextWindow", 4096, "numPredict", 128),
                status,
                passed,
                score == null ? null : new BigDecimal(score),
                List.of(),
                null,
                now,
                null,
                now,
                now
        );
    }
}