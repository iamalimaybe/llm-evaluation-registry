package com.aliniaz.llmeval.prompt.service.impl;

import com.aliniaz.llmeval.common.exception.DuplicateResourceException;
import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.prompt.api.request.CreatePromptVersionRequest;
import com.aliniaz.llmeval.prompt.api.response.PromptVersionResponse;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.prompt.domain.PromptVersionRepository;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
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
class PromptVersionServiceImplTest {

    @Mock
    private PromptVersionRepository promptVersionRepository;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private PromptVersionServiceImpl promptVersionService;

    @Test
    void createPromptVersionShouldSavePromptVersionWhenVersionIsUnique() {
        AiWorkflow workflow = new AiWorkflow(
                "RAG Document Assistant",
                "Evaluates grounded document question answering workflows"
        );
        workflow.setId(1L);

        CreatePromptVersionRequest request = new CreatePromptVersionRequest(
                "v1",
                "Answer only from the provided document context.",
                "Initial grounded answer prompt"
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(promptVersionRepository.existsByWorkflowIdAndVersionIgnoreCase(1L, "v1"))
                .thenReturn(false);
        when(promptVersionRepository.save(any(PromptVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PromptVersionResponse response = promptVersionService.createPromptVersion(1L, request);

        assertThat(response.workflowId()).isEqualTo(1L);
        assertThat(response.version()).isEqualTo("v1");
        assertThat(response.promptText()).isEqualTo("Answer only from the provided document context.");
        assertThat(response.changeNotes()).isEqualTo("Initial grounded answer prompt");

        verify(workflowService).getWorkflowEntity(1L);
        verify(promptVersionRepository).existsByWorkflowIdAndVersionIgnoreCase(1L, "v1");
        verify(promptVersionRepository).save(any(PromptVersion.class));
    }

    @Test
    void createPromptVersionShouldThrowDuplicateResourceExceptionWhenVersionAlreadyExists() {
        AiWorkflow workflow = new AiWorkflow(
                "RAG Document Assistant",
                "Evaluates grounded document question answering workflows"
        );
        workflow.setId(1L);

        CreatePromptVersionRequest request = new CreatePromptVersionRequest(
                "v1",
                "Updated prompt text",
                "Duplicate version"
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(promptVersionRepository.existsByWorkflowIdAndVersionIgnoreCase(1L, "v1"))
                .thenReturn(true);

        assertThatThrownBy(() -> promptVersionService.createPromptVersion(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Prompt version already exists for workflow id 1: v1");

        verify(workflowService).getWorkflowEntity(1L);
        verify(promptVersionRepository).existsByWorkflowIdAndVersionIgnoreCase(1L, "v1");
    }

    @Test
    void getPromptVersionShouldReturnPromptVersionWhenFound() {
        AiWorkflow workflow = new AiWorkflow(
                "AI Product Recommendation Service",
                "Evaluates recommendation quality against product facts"
        );
        workflow.setId(2L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Recommend products using only catalog facts.",
                "Initial recommendation prompt"
        );
        promptVersion.setId(10L);

        when(workflowService.getWorkflowEntity(2L)).thenReturn(workflow);
        when(promptVersionRepository.findByIdAndWorkflowId(10L, 2L))
                .thenReturn(Optional.of(promptVersion));

        PromptVersionResponse response = promptVersionService.getPromptVersion(2L, 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.workflowId()).isEqualTo(2L);
        assertThat(response.version()).isEqualTo("v1");
        assertThat(response.promptText()).isEqualTo("Recommend products using only catalog facts.");

        verify(workflowService).getWorkflowEntity(2L);
        verify(promptVersionRepository).findByIdAndWorkflowId(10L, 2L);
    }

    @Test
    void getPromptVersionShouldThrowResourceNotFoundExceptionWhenMissing() {
        AiWorkflow workflow = new AiWorkflow(
                "n8n Business Workflow Automation System",
                "Evaluates AI-assisted workflow automation behavior"
        );
        workflow.setId(3L);

        when(workflowService.getWorkflowEntity(3L)).thenReturn(workflow);
        when(promptVersionRepository.findByIdAndWorkflowId(99L, 3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> promptVersionService.getPromptVersion(3L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Prompt version not found with id: 99");

        verify(workflowService).getWorkflowEntity(3L);
        verify(promptVersionRepository).findByIdAndWorkflowId(99L, 3L);
    }
}