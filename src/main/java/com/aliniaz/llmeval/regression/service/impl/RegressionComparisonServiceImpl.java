package com.aliniaz.llmeval.regression.service.impl;

import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import com.aliniaz.llmeval.regression.api.response.EvaluationRunComparisonResponse;
import com.aliniaz.llmeval.regression.service.RegressionComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegressionComparisonServiceImpl implements RegressionComparisonService {

    private static final String OUTCOME_IMPROVED = "IMPROVED";
    private static final String OUTCOME_REGRESSED = "REGRESSED";
    private static final String OUTCOME_UNCHANGED = "UNCHANGED";
    private static final String OUTCOME_NOT_COMPARABLE = "NOT_COMPARABLE";

    private final EvaluationRunService evaluationRunService;

    @Override
    public EvaluationRunComparisonResponse compareRuns(
            Long workflowId,
            Long baselineRunId,
            Long candidateRunId
    ) {
        EvaluationRunResponse baselineRun = evaluationRunService.getEvaluationRun(workflowId, baselineRunId);
        EvaluationRunResponse candidateRun = evaluationRunService.getEvaluationRun(workflowId, candidateRunId);

        BigDecimal baselineScore = baselineRun.score();
        BigDecimal candidateScore = candidateRun.score();

        BigDecimal scoreDelta = null;
        if (baselineScore != null && candidateScore != null) {
            scoreDelta = candidateScore.subtract(baselineScore);
        }

        List<String> regressionReasons = buildRegressionReasons(
                baselineRun,
                candidateRun,
                scoreDelta
        );

        String outcome = determineOutcome(
                baselineRun,
                candidateRun,
                scoreDelta,
                regressionReasons
        );

        return new EvaluationRunComparisonResponse(
                workflowId,
                baselineRunId,
                candidateRunId,
                baselineScore,
                candidateScore,
                scoreDelta,
                baselineRun.passed(),
                candidateRun.passed(),
                outcome,
                regressionReasons
        );
    }

    private List<String> buildRegressionReasons(
            EvaluationRunResponse baselineRun,
            EvaluationRunResponse candidateRun,
            BigDecimal scoreDelta
    ) {
        List<String> reasons = new ArrayList<>();

        if (baselineRun.passed() == null) {
            reasons.add("Baseline run has no pass/fail result.");
        }

        if (candidateRun.passed() == null) {
            reasons.add("Candidate run has no pass/fail result.");
        }

        if (baselineRun.score() == null) {
            reasons.add("Baseline run has no score.");
        }

        if (candidateRun.score() == null) {
            reasons.add("Candidate run has no score.");
        }

        if (Boolean.TRUE.equals(baselineRun.passed()) && Boolean.FALSE.equals(candidateRun.passed())) {
            reasons.add("Candidate failed while baseline passed.");
        }

        if (scoreDelta != null && scoreDelta.compareTo(BigDecimal.ZERO) < 0) {
            reasons.add("Candidate score is lower than baseline score.");
        }

        return reasons;
    }

    private String determineOutcome(
            EvaluationRunResponse baselineRun,
            EvaluationRunResponse candidateRun,
            BigDecimal scoreDelta,
            List<String> regressionReasons
    ) {
        if (baselineRun.passed() == null
                || candidateRun.passed() == null
                || baselineRun.score() == null
                || candidateRun.score() == null) {
            return OUTCOME_NOT_COMPARABLE;
        }

        if (Boolean.TRUE.equals(baselineRun.passed()) && Boolean.FALSE.equals(candidateRun.passed())) {
            return OUTCOME_REGRESSED;
        }

        if (Boolean.FALSE.equals(baselineRun.passed()) && Boolean.TRUE.equals(candidateRun.passed())) {
            return OUTCOME_IMPROVED;
        }

        if (!regressionReasons.isEmpty()) {
            return OUTCOME_REGRESSED;
        }

        if (scoreDelta.compareTo(BigDecimal.ZERO) > 0) {
            return OUTCOME_IMPROVED;
        }

        return OUTCOME_UNCHANGED;
    }
}