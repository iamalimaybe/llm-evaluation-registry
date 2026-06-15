package com.aliniaz.llmeval.evaluationbatch.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationBatchRepository extends JpaRepository<EvaluationBatch, Long> {

    List<EvaluationBatch> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    Optional<EvaluationBatch> findByIdAndWorkflowId(Long id, Long workflowId);

    Optional<EvaluationBatch> findFirstByStatusOrderByCreatedAtAsc(EvaluationBatchStatus status);

    boolean existsByStatusIn(Collection<EvaluationBatchStatus> statuses);
}