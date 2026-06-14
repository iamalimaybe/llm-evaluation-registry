package com.aliniaz.llmeval.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiWorkflowRepository extends JpaRepository<AiWorkflow, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<AiWorkflow> findByIdAndStatus(Long id, WorkflowStatus status);
}