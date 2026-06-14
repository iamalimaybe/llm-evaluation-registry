package com.aliniaz.llmeval.regression.service.impl;

import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunStatus;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import com.aliniaz.llmeval.regression.api.response.EvaluationRunComparisonResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegressionComparisonServiceImplTest {

    @Mock
    private EvaluationRunService evaluationRunService;

    @InjectMocks
    private RegressionComparisonServiceImpl regressionComparisonService;

    @Test
    void compareRunsReturnsRegressedWhenCandidateFailsAndScoreDrops() {
        Long workflowId = 2L;
        Long baselineRunId = 4L;
        Long candidateRunId = 5L;

        when(evaluationRunService.getEvaluationRun(workflowId, baselineRunId))
                .thenReturn(evaluationRun(
                        baselineRunId,
                        workflowId,
                        true,
                        new BigDecimal("0.92"),
                        EvaluationRunStatus.PASSED
                ));

        when(evaluationRunService.getEvaluationRun(workflowId, candidateRunId))
                .thenReturn(evaluationRun(
                        candidateRunId,
                        workflowId,
                        false,
                        new BigDecimal("0.60"),
                        EvaluationRunStatus.FAILED
                ));

        EvaluationRunComparisonResponse response = regressionComparisonService.compareRuns(
                workflowId,
                baselineRunId,
                candidateRunId
        );

        assertThat(response.workflowId()).isEqualTo(workflowId);
        assertThat(response.baselineRunId()).isEqualTo(baselineRunId);
        assertThat(response.candidateRunId()).isEqualTo(candidateRunId);
        assertThat(response.baselineScore()).isEqualByComparingTo("0.92");
        assertThat(response.candidateScore()).isEqualByComparingTo("0.60");
        assertThat(response.scoreDelta()).isEqualByComparingTo("-0.32");
        assertThat(response.baselinePassed()).isTrue();
        assertThat(response.candidatePassed()).isFalse();
        assertThat(response.outcome()).isEqualTo("REGRESSED");
        assertThat(response.regressionReasons()).containsExactly(
                "Candidate failed while baseline passed.",
                "Candidate score is lower than baseline score."
        );
    }

    @Test
    void compareRunsReturnsImprovedWhenCandidatePassesAndBaselineFails() {
        Long workflowId = 2L;
        Long baselineRunId = 4L;
        Long candidateRunId = 5L;

        when(evaluationRunService.getEvaluationRun(workflowId, baselineRunId))
                .thenReturn(evaluationRun(
                        baselineRunId,
                        workflowId,
                        false,
                        new BigDecimal("0.60"),
                        EvaluationRunStatus.FAILED
                ));

        when(evaluationRunService.getEvaluationRun(workflowId, candidateRunId))
                .thenReturn(evaluationRun(
                        candidateRunId,
                        workflowId,
                        true,
                        new BigDecimal("0.92"),
                        EvaluationRunStatus.PASSED
                ));

        EvaluationRunComparisonResponse response = regressionComparisonService.compareRuns(
                workflowId,
                baselineRunId,
                candidateRunId
        );

        assertThat(response.scoreDelta()).isEqualByComparingTo("0.32");
        assertThat(response.outcome()).isEqualTo("IMPROVED");
        assertThat(response.regressionReasons()).isEmpty();
    }

    @Test
    void compareRunsReturnsNotComparableWhenResultDataIsMissing() {
        Long workflowId = 2L;
        Long baselineRunId = 4L;
        Long candidateRunId = 5L;

        when(evaluationRunService.getEvaluationRun(workflowId, baselineRunId))
                .thenReturn(evaluationRun(
                        baselineRunId,
                        workflowId,
                        null,
                        null,
                        EvaluationRunStatus.PENDING
                ));

        when(evaluationRunService.getEvaluationRun(workflowId, candidateRunId))
                .thenReturn(evaluationRun(
                        candidateRunId,
                        workflowId,
                        true,
                        new BigDecimal("0.92"),
                        EvaluationRunStatus.PASSED
                ));

        EvaluationRunComparisonResponse response = regressionComparisonService.compareRuns(
                workflowId,
                baselineRunId,
                candidateRunId
        );

        assertThat(response.scoreDelta()).isNull();
        assertThat(response.outcome()).isEqualTo("NOT_COMPARABLE");
        assertThat(response.regressionReasons()).contains(
                "Baseline run has no pass/fail result.",
                "Baseline run has no score."
        );
    }

    private EvaluationRunResponse evaluationRun(
            Long id,
            Long workflowId,
            Boolean passed,
            BigDecimal score,
            EvaluationRunStatus status
    ) {
        LocalDateTime now = LocalDateTime.now();

        return new EvaluationRunResponse(
                id,
                workflowId,
                2L,
                2L,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                null,
                null,
                null,
                new BigDecimal("0.20"),
                Map.of("contextWindow", 4096, "numPredict", 512),
                status,
                passed,
                score,
                List.of(),
                null,
                now,
                null,
                now,
                now
        );
    }
}