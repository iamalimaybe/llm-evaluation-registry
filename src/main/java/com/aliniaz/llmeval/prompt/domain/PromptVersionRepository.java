package com.aliniaz.llmeval.prompt.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {

    boolean existsByWorkflowIdAndVersionIgnoreCase(Long workflowId, String version);

    List<PromptVersion> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);

    Optional<PromptVersion> findByIdAndWorkflowId(Long id, Long workflowId);
}