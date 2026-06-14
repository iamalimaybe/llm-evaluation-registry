package com.aliniaz.llmeval.prompt.service.impl;

import com.aliniaz.llmeval.common.exception.DuplicateResourceException;
import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.prompt.api.request.CreatePromptVersionRequest;
import com.aliniaz.llmeval.prompt.api.response.PromptVersionResponse;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.prompt.domain.PromptVersionRepository;
import com.aliniaz.llmeval.prompt.service.PromptVersionService;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PromptVersionServiceImpl implements PromptVersionService {

    private final PromptVersionRepository promptVersionRepository;
    private final WorkflowService workflowService;

    @Override
    public PromptVersionResponse createPromptVersion(Long workflowId, CreatePromptVersionRequest request) {
        AiWorkflow workflow = workflowService.getWorkflowEntity(workflowId);

        if (promptVersionRepository.existsByWorkflowIdAndVersionIgnoreCase(workflowId, request.version())) {
            throw new DuplicateResourceException(
                    "Prompt version already exists for workflow id " + workflowId + ": " + request.version()
            );
        }

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                request.version(),
                request.promptText(),
                request.changeNotes()
        );

        PromptVersion savedPromptVersion = promptVersionRepository.save(promptVersion);

        return toResponse(savedPromptVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromptVersionResponse> getPromptVersions(Long workflowId) {
        workflowService.getWorkflowEntity(workflowId);

        return promptVersionRepository.findByWorkflowIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromptVersionResponse getPromptVersion(Long workflowId, Long promptVersionId) {
        workflowService.getWorkflowEntity(workflowId);

        return promptVersionRepository.findByIdAndWorkflowId(promptVersionId, workflowId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Prompt version not found with id: " + promptVersionId
                ));
    }

    private PromptVersionResponse toResponse(PromptVersion promptVersion) {
        return new PromptVersionResponse(
                promptVersion.getId(),
                promptVersion.getWorkflow().getId(),
                promptVersion.getVersion(),
                promptVersion.getPromptText(),
                promptVersion.getChangeNotes(),
                promptVersion.getStatus(),
                promptVersion.getCreatedAt(),
                promptVersion.getUpdatedAt()
        );
    }
}