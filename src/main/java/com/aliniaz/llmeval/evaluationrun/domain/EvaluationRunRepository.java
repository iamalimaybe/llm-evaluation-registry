package com.aliniaz.llmeval.evaluationrun.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, Long> {

    List<EvaluationRun> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    List<EvaluationRun> findByWorkflowIdAndPromptVersionIdOrderByCreatedAtDesc(
            Long workflowId,
            Long promptVersionId
    );

    List<EvaluationRun> findByWorkflowIdAndEvaluationCaseIdOrderByCreatedAtDesc(
            Long workflowId,
            Long evaluationCaseId
    );

    Optional<EvaluationRun> findByIdAndWorkflowId(Long id, Long workflowId);

    List<EvaluationRun> findByBatchIdOrderByCreatedAtAsc(Long batchId);
}