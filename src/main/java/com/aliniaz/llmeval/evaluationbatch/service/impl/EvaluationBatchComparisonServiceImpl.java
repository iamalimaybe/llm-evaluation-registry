package com.aliniaz.llmeval.evaluationbatch.service.impl;

import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchCaseComparisonResponse;
import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchComparisonResponse;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatch;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchRepository;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchStatus;
import com.aliniaz.llmeval.evaluationbatch.service.EvaluationBatchComparisonService;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunStatus;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunService;
import com.aliniaz.llmeval.regression.domain.RegressionComparisonOutcome;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationBatchComparisonServiceImpl implements EvaluationBatchComparisonService {

    private final EvaluationBatchRepository evaluationBatchRepository;
    private final EvaluationRunService evaluationRunService;
    private final WorkflowService workflowService;

    @Override
    public EvaluationBatchComparisonResponse compareEvaluationBatches(
            Long workflowId,
            Long baselineBatchId,
            Long candidateBatchId
    ) {
        workflowService.getWorkflowEntity(workflowId);

        EvaluationBatch baselineBatch = getEvaluationBatch(workflowId, baselineBatchId);
        EvaluationBatch candidateBatch = getEvaluationBatch(workflowId, candidateBatchId);

        List<EvaluationRunResponse> baselineRuns = evaluationRunService.getEvaluationRunsByBatch(
                workflowId,
                baselineBatchId
        );

        List<EvaluationRunResponse> candidateRuns = evaluationRunService.getEvaluationRunsByBatch(
                workflowId,
                candidateBatchId
        );

        List<String> comparisonReasons = new ArrayList<>();

        List<EvaluationBatchCaseComparisonResponse> caseComparisons = compareCases(
                baselineRuns,
                candidateRuns
        );

        boolean comparable = isComparable(
                baselineBatch,
                candidateBatch,
                baselineRuns,
                candidateRuns,
                comparisonReasons
        );

        RegressionComparisonOutcome outcome = comparable
                ? determineOutcome(baselineBatch, candidateBatch, caseComparisons, comparisonReasons)
                : RegressionComparisonOutcome.NOT_COMPARABLE;

        if (!comparable && comparisonReasons.isEmpty()) {
            comparisonReasons.add("Batches are not comparable.");
        }

        return new EvaluationBatchComparisonResponse(
                workflowId,
                baselineBatch.getId(),
                candidateBatch.getId(),
                baselineBatch.getStatus(),
                candidateBatch.getStatus(),
                baselineBatch.getTotalRuns(),
                candidateBatch.getTotalRuns(),
                baselineBatch.getCompletedRuns(),
                candidateBatch.getCompletedRuns(),
                baselineBatch.getPassedRuns(),
                candidateBatch.getPassedRuns(),
                baselineBatch.getFailedRuns(),
                candidateBatch.getFailedRuns(),
                baselineBatch.getErroredRuns(),
                candidateBatch.getErroredRuns(),
                zeroIfNull(baselineBatch.getAverageScore()),
                zeroIfNull(candidateBatch.getAverageScore()),
                delta(candidateBatch.getAverageScore(), baselineBatch.getAverageScore()),
                outcome,
                comparisonReasons,
                caseComparisons
        );
    }

    private EvaluationBatch getEvaluationBatch(Long workflowId, Long batchId) {
        return evaluationBatchRepository.findByIdAndWorkflowId(batchId, workflowId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Evaluation batch not found with id: " + batchId
                ));
    }

    private boolean isComparable(
            EvaluationBatch baselineBatch,
            EvaluationBatch candidateBatch,
            List<EvaluationRunResponse> baselineRuns,
            List<EvaluationRunResponse> candidateRuns,
            List<String> comparisonReasons
    ) {
        boolean comparable = true;

        if (!isComparableStatus(baselineBatch.getStatus())) {
            comparisonReasons.add("Baseline batch is not completed.");
            comparable = false;
        }

        if (!isComparableStatus(candidateBatch.getStatus())) {
            comparisonReasons.add("Candidate batch is not completed.");
            comparable = false;
        }

        if (baselineBatch.getTotalRuns() != candidateBatch.getTotalRuns()) {
            comparisonReasons.add("Batch total run counts differ.");
            comparable = false;
        }

        if (baselineBatch.getCompletedRuns() != candidateBatch.getCompletedRuns()) {
            comparisonReasons.add("Batch completed run counts differ.");
            comparable = false;
        }

        if (baselineRuns.size() != candidateRuns.size()) {
            comparisonReasons.add("Persisted batch run counts differ.");
            comparable = false;
        }

        if (!caseIds(baselineRuns).equals(caseIds(candidateRuns))) {
            comparisonReasons.add("Evaluation case sets differ between batches.");
            comparable = false;
        }

        return comparable;
    }

    private boolean isComparableStatus(EvaluationBatchStatus status) {
        return status == EvaluationBatchStatus.COMPLETED
                || status == EvaluationBatchStatus.COMPLETED_WITH_FAILURES;
    }

    private List<EvaluationBatchCaseComparisonResponse> compareCases(
            List<EvaluationRunResponse> baselineRuns,
            List<EvaluationRunResponse> candidateRuns
    ) {
        Map<Long, EvaluationRunResponse> baselineRunsByCaseId = byEvaluationCaseId(baselineRuns);
        Map<Long, EvaluationRunResponse> candidateRunsByCaseId = byEvaluationCaseId(candidateRuns);

        TreeSet<Long> evaluationCaseIds = new TreeSet<>();
        evaluationCaseIds.addAll(baselineRunsByCaseId.keySet());
        evaluationCaseIds.addAll(candidateRunsByCaseId.keySet());

        List<EvaluationBatchCaseComparisonResponse> comparisons = new ArrayList<>();

        for (Long evaluationCaseId : evaluationCaseIds) {
            EvaluationRunResponse baselineRun = baselineRunsByCaseId.get(evaluationCaseId);
            EvaluationRunResponse candidateRun = candidateRunsByCaseId.get(evaluationCaseId);

            comparisons.add(compareCase(evaluationCaseId, baselineRun, candidateRun));
        }

        return comparisons;
    }

    private Map<Long, EvaluationRunResponse> byEvaluationCaseId(List<EvaluationRunResponse> runs) {
        Map<Long, EvaluationRunResponse> result = new LinkedHashMap<>();

        runs.stream()
                .sorted(Comparator.comparing(EvaluationRunResponse::evaluationCaseId))
                .forEach(run -> result.putIfAbsent(run.evaluationCaseId(), run));

        return result;
    }

    private EvaluationBatchCaseComparisonResponse compareCase(
            Long evaluationCaseId,
            EvaluationRunResponse baselineRun,
            EvaluationRunResponse candidateRun
    ) {
        List<String> regressionReasons = new ArrayList<>();

        if (baselineRun == null || candidateRun == null) {
            regressionReasons.add("Evaluation case is missing from one of the batches.");

            return new EvaluationBatchCaseComparisonResponse(
                    evaluationCaseId,
                    baselineRun == null ? null : baselineRun.id(),
                    candidateRun == null ? null : candidateRun.id(),
                    baselineRun == null ? null : baselineRun.status(),
                    candidateRun == null ? null : candidateRun.status(),
                    baselineRun == null ? null : baselineRun.passed(),
                    candidateRun == null ? null : candidateRun.passed(),
                    baselineRun == null ? null : baselineRun.score(),
                    candidateRun == null ? null : candidateRun.score(),
                    null,
                    RegressionComparisonOutcome.NOT_COMPARABLE,
                    regressionReasons
            );
        }

        RegressionComparisonOutcome outcome = determineCaseOutcome(
                baselineRun,
                candidateRun,
                regressionReasons
        );

        return new EvaluationBatchCaseComparisonResponse(
                evaluationCaseId,
                baselineRun.id(),
                candidateRun.id(),
                baselineRun.status(),
                candidateRun.status(),
                baselineRun.passed(),
                candidateRun.passed(),
                baselineRun.score(),
                candidateRun.score(),
                delta(candidateRun.score(), baselineRun.score()),
                outcome,
                regressionReasons
        );
    }

    private RegressionComparisonOutcome determineCaseOutcome(
            EvaluationRunResponse baselineRun,
            EvaluationRunResponse candidateRun,
            List<String> regressionReasons
    ) {
        if (candidateRun.status() == EvaluationRunStatus.ERROR && baselineRun.status() != EvaluationRunStatus.ERROR) {
            regressionReasons.add("Candidate run errored while baseline run did not.");
            addCandidateFailureReasons(candidateRun, regressionReasons);
            return RegressionComparisonOutcome.REGRESSED;
        }

        if (baselineRun.status() == EvaluationRunStatus.ERROR && candidateRun.status() != EvaluationRunStatus.ERROR) {
            return RegressionComparisonOutcome.IMPROVED;
        }

        int scoreComparison = zeroIfNull(candidateRun.score()).compareTo(zeroIfNull(baselineRun.score()));

        if (Boolean.TRUE.equals(baselineRun.passed()) && !Boolean.TRUE.equals(candidateRun.passed())) {
            regressionReasons.add("Candidate run failed while baseline run passed.");

            if (scoreComparison < 0) {
                regressionReasons.add("Candidate score is lower than baseline score.");
            }

            addCandidateFailureReasons(candidateRun, regressionReasons);
            return RegressionComparisonOutcome.REGRESSED;
        }

        if (!Boolean.TRUE.equals(baselineRun.passed()) && Boolean.TRUE.equals(candidateRun.passed())) {
            return RegressionComparisonOutcome.IMPROVED;
        }

        if (scoreComparison < 0) {
            regressionReasons.add("Candidate score is lower than baseline score.");
            addCandidateFailureReasons(candidateRun, regressionReasons);
            return RegressionComparisonOutcome.REGRESSED;
        }

        if (scoreComparison > 0) {
            return RegressionComparisonOutcome.IMPROVED;
        }

        return RegressionComparisonOutcome.UNCHANGED;
    }

    private RegressionComparisonOutcome determineOutcome(
            EvaluationBatch baselineBatch,
            EvaluationBatch candidateBatch,
            List<EvaluationBatchCaseComparisonResponse> caseComparisons,
            List<String> comparisonReasons
    ) {
        boolean regressed = false;
        boolean improved = false;

        if (candidateBatch.getErroredRuns() > baselineBatch.getErroredRuns()) {
            comparisonReasons.add("Candidate batch has more errored runs.");
            regressed = true;
        }

        if (candidateBatch.getFailedRuns() > baselineBatch.getFailedRuns()) {
            comparisonReasons.add("Candidate batch has more failed runs.");
            regressed = true;
        }

        if (candidateBatch.getPassedRuns() < baselineBatch.getPassedRuns()) {
            comparisonReasons.add("Candidate batch has fewer passed runs.");
            regressed = true;
        }

        int averageScoreComparison = zeroIfNull(candidateBatch.getAverageScore())
                .compareTo(zeroIfNull(baselineBatch.getAverageScore()));

        if (averageScoreComparison < 0) {
            comparisonReasons.add("Candidate average score is lower than baseline average score.");
            regressed = true;
        }

        if (caseComparisons.stream().anyMatch(
                comparison -> comparison.outcome() == RegressionComparisonOutcome.REGRESSED
        )) {
            comparisonReasons.add("One or more evaluation cases regressed.");
            regressed = true;
        }

        if (regressed) {
            return RegressionComparisonOutcome.REGRESSED;
        }

        if (candidateBatch.getErroredRuns() < baselineBatch.getErroredRuns()) {
            comparisonReasons.add("Candidate batch has fewer errored runs.");
            improved = true;
        }

        if (candidateBatch.getFailedRuns() < baselineBatch.getFailedRuns()) {
            comparisonReasons.add("Candidate batch has fewer failed runs.");
            improved = true;
        }

        if (candidateBatch.getPassedRuns() > baselineBatch.getPassedRuns()) {
            comparisonReasons.add("Candidate batch has more passed runs.");
            improved = true;
        }

        if (averageScoreComparison > 0) {
            comparisonReasons.add("Candidate average score is higher than baseline average score.");
            improved = true;
        }

        if (caseComparisons.stream().anyMatch(
                comparison -> comparison.outcome() == RegressionComparisonOutcome.IMPROVED
        )) {
            comparisonReasons.add("One or more evaluation cases improved.");
            improved = true;
        }

        if (improved) {
            return RegressionComparisonOutcome.IMPROVED;
        }

        comparisonReasons.add("Candidate batch matched baseline score and run outcome counts.");
        return RegressionComparisonOutcome.UNCHANGED;
    }

    private List<Long> caseIds(List<EvaluationRunResponse> runs) {
        return runs.stream()
                .map(EvaluationRunResponse::evaluationCaseId)
                .sorted()
                .toList();
    }

    private void addCandidateFailureReasons(
            EvaluationRunResponse candidateRun,
            List<String> regressionReasons
    ) {
        if (candidateRun.failureReasons() == null || candidateRun.failureReasons().isEmpty()) {
            return;
        }

        regressionReasons.add("Candidate failure reasons: " + String.join("; ", candidateRun.failureReasons()));
    }

    private BigDecimal delta(BigDecimal candidateValue, BigDecimal baselineValue) {
        return zeroIfNull(candidateValue)
                .subtract(zeroIfNull(baselineValue))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}