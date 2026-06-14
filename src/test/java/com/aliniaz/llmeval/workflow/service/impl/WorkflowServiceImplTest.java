package com.aliniaz.llmeval.workflow.service.impl;

import com.aliniaz.llmeval.common.exception.DuplicateResourceException;
import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.workflow.api.request.CreateWorkflowRequest;
import com.aliniaz.llmeval.workflow.api.response.WorkflowResponse;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.domain.AiWorkflowRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceImplTest {

    @Mock
    private AiWorkflowRepository workflowRepository;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    @Test
    void createWorkflowShouldSaveWorkflowWhenNameIsUnique() {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
                "RAG Document Assistant",
                "Evaluates grounded document question answering workflows"
        );

        when(workflowRepository.existsByNameIgnoreCase("RAG Document Assistant"))
                .thenReturn(false);

        when(workflowRepository.save(any(AiWorkflow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkflowResponse response = workflowService.createWorkflow(request);

        assertThat(response.name()).isEqualTo("RAG Document Assistant");
        assertThat(response.description()).isEqualTo("Evaluates grounded document question answering workflows");

        verify(workflowRepository).existsByNameIgnoreCase("RAG Document Assistant");
        verify(workflowRepository).save(any(AiWorkflow.class));
    }

    @Test
    void createWorkflowShouldThrowDuplicateResourceExceptionWhenNameAlreadyExists() {
        CreateWorkflowRequest request = new CreateWorkflowRequest(
                "RAG Document Assistant",
                "Duplicate workflow"
        );

        when(workflowRepository.existsByNameIgnoreCase("RAG Document Assistant"))
                .thenReturn(true);

        assertThatThrownBy(() -> workflowService.createWorkflow(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Workflow already exists with name: RAG Document Assistant");

        verify(workflowRepository).existsByNameIgnoreCase("RAG Document Assistant");
    }

    @Test
    void getWorkflowShouldReturnWorkflowWhenFound() {
        AiWorkflow workflow = new AiWorkflow(
                "AI Product Recommendation Service",
                "Evaluates recommendation quality against product facts"
        );

        when(workflowRepository.findById(1L))
                .thenReturn(Optional.of(workflow));

        WorkflowResponse response = workflowService.getWorkflow(1L);

        assertThat(response.name()).isEqualTo("AI Product Recommendation Service");
        assertThat(response.description()).isEqualTo("Evaluates recommendation quality against product facts");

        verify(workflowRepository).findById(1L);
    }

    @Test
    void getWorkflowShouldThrowResourceNotFoundExceptionWhenMissing() {
        when(workflowRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> workflowService.getWorkflow(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workflow not found with id: 99");

        verify(workflowRepository).findById(99L);
    }
}