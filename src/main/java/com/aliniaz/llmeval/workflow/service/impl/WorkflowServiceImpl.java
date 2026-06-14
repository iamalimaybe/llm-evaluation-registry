package com.aliniaz.llmeval.workflow.service.impl;

import com.aliniaz.llmeval.common.exception.DuplicateResourceException;
import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.workflow.api.request.CreateWorkflowRequest;
import com.aliniaz.llmeval.workflow.api.response.WorkflowResponse;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.domain.AiWorkflowRepository;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private final AiWorkflowRepository workflowRepository;

    @Override
    public WorkflowResponse createWorkflow(CreateWorkflowRequest request) {
        if (workflowRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Workflow already exists with name: " + request.name());
        }

        AiWorkflow workflow = new AiWorkflow(request.name(), request.description());
        AiWorkflow savedWorkflow = workflowRepository.save(workflow);

        return toResponse(savedWorkflow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowResponse> getWorkflows() {
        return workflowRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(Long id) {
        return toResponse(getWorkflowEntity(id));
    }

    private WorkflowResponse toResponse(AiWorkflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getDescription(),
                workflow.getStatus(),
                workflow.getCreatedAt(),
                workflow.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AiWorkflow getWorkflowEntity(Long id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found with id: " + id));
    }
}