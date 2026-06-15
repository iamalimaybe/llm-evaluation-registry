package com.aliniaz.llmeval.evaluationcase.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationCaseRepository extends JpaRepository<EvaluationCase, Long> {

    boolean existsByWorkflowIdAndNameIgnoreCase(Long workflowId, String name);

    List<EvaluationCase> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    List<EvaluationCase> findByWorkflowIdAndEnabledTrueOrderByCreatedAtDesc(Long workflowId);

    Optional<EvaluationCase> findByIdAndWorkflowId(Long id, Long workflowId);

    List<EvaluationCase> findByWorkflowIdAndEnabledTrueOrderByCreatedAtAsc(Long workflowId);
}