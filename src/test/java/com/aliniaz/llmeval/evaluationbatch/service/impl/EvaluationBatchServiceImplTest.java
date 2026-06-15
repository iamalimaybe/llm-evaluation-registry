package com.aliniaz.llmeval.evaluationbatch.service.impl;

import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationbatch.api.request.CreateEvaluationBatchRequest;
import com.aliniaz.llmeval.evaluationbatch.api.response.EvaluationBatchResponse;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatch;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchRepository;
import com.aliniaz.llmeval.evaluationbatch.domain.EvaluationBatchStatus;
import com.aliniaz.llmeval.evaluationbatch.service.EvaluationBatchWorker;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.prompt.service.PromptVersionService;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationBatchServiceImplTest {

    @Mock
    private EvaluationBatchRepository evaluationBatchRepository;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private PromptVersionService promptVersionService;

    @Mock
    private EvaluationCaseService evaluationCaseService;

    @Mock
    private EvaluationBatchWorker evaluationBatchWorker;

    @InjectMocks
    private EvaluationBatchServiceImpl evaluationBatchService;

    @Test
    void createEvaluationBatchShouldCreateQueuedBatchAndTriggerWorker() {
        AiWorkflow workflow = new AiWorkflow(
                "Shipping Support Evaluation",
                "Evaluates shipping status extraction."
        );
        workflow.setId(1L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Extract shipping status.",
                "Initial prompt."
        );
        promptVersion.setId(10L);

        EvaluationCase firstCase = new EvaluationCase(
                workflow,
                "Missing shipping status",
                "EXTRACTION",
                Map.of("message", "Where is my order?"),
                Map.of("status", "INSUFFICIENT_INFORMATION"),
                null,
                null,
                null
        );
        firstCase.setId(100L);

        EvaluationCase secondCase = new EvaluationCase(
                workflow,
                "Delivered status",
                "EXTRACTION",
                Map.of("message", "Order was delivered."),
                Map.of("status", "DELIVERED"),
                null,
                null,
                null
        );
        secondCase.setId(101L);

        CreateEvaluationBatchRequest request = new CreateEvaluationBatchRequest(
                10L,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                BigDecimal.ZERO,
                Map.of("numPredict", 128, "contextWindow", 4096)
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(promptVersionService.getPromptVersionEntity(1L, 10L)).thenReturn(promptVersion);
        when(evaluationCaseService.getEnabledEvaluationCaseEntities(1L))
                .thenReturn(List.of(firstCase, secondCase));
        when(evaluationBatchRepository.save(any(EvaluationBatch.class)))
                .thenAnswer(invocation -> {
                    EvaluationBatch batch = invocation.getArgument(0);
                    batch.setId(500L);
                    return batch;
                });

        EvaluationBatchResponse response = evaluationBatchService.createEvaluationBatch(1L, request);

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.workflowId()).isEqualTo(1L);
        assertThat(response.promptVersionId()).isEqualTo(10L);
        assertThat(response.modelName()).isEqualTo("qwen3:4b");
        assertThat(response.provider()).isEqualTo(EvaluationRunProvider.OLLAMA);
        assertThat(response.status()).isEqualTo(EvaluationBatchStatus.QUEUED);
        assertThat(response.totalRuns()).isEqualTo(2);
        assertThat(response.completedRuns()).isZero();

        verify(evaluationBatchRepository).save(any(EvaluationBatch.class));
        verify(evaluationBatchWorker).processQueuedBatches();
    }

    @Test
    void createEvaluationBatchShouldFailWhenNoEnabledEvaluationCasesExist() {
        AiWorkflow workflow = new AiWorkflow(
                "Empty Workflow",
                "No enabled cases."
        );
        workflow.setId(1L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Prompt",
                "Notes"
        );
        promptVersion.setId(10L);

        CreateEvaluationBatchRequest request = new CreateEvaluationBatchRequest(
                10L,
                "qwen3:4b",
                null,
                null,
                BigDecimal.ZERO,
                Map.of("numPredict", 128)
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(promptVersionService.getPromptVersionEntity(1L, 10L)).thenReturn(promptVersion);
        when(evaluationCaseService.getEnabledEvaluationCaseEntities(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> evaluationBatchService.createEvaluationBatch(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot create evaluation batch because workflow has no enabled evaluation cases.");
    }

    @Test
    void cancelEvaluationBatchShouldCancelQueuedBatch() {
        AiWorkflow workflow = new AiWorkflow(
                "Shipping Support Evaluation",
                "Evaluates shipping status extraction."
        );
        workflow.setId(1L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Extract shipping status.",
                "Initial prompt."
        );
        promptVersion.setId(10L);

        EvaluationBatch batch = new EvaluationBatch(
                workflow,
                promptVersion,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                BigDecimal.ZERO,
                Map.of("numPredict", 128),
                2
        );
        batch.setId(500L);

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(evaluationBatchRepository.findByIdAndWorkflowId(500L, 1L))
                .thenReturn(Optional.of(batch));

        EvaluationBatchResponse response = evaluationBatchService.cancelEvaluationBatch(1L, 500L);

        assertThat(response.status()).isEqualTo(EvaluationBatchStatus.CANCELLED);
        assertThat(response.completedAt()).isNotNull();
    }

    @Test
    void cancelEvaluationBatchShouldMarkRunningBatchAsCancelRequested() {
        AiWorkflow workflow = new AiWorkflow(
                "Shipping Support Evaluation",
                "Evaluates shipping status extraction."
        );
        workflow.setId(1L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Extract shipping status.",
                "Initial prompt."
        );
        promptVersion.setId(10L);

        EvaluationBatch batch = new EvaluationBatch(
                workflow,
                promptVersion,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                BigDecimal.ZERO,
                Map.of("numPredict", 128),
                2
        );
        batch.setId(500L);
        batch.markRunning();

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(evaluationBatchRepository.findByIdAndWorkflowId(500L, 1L))
                .thenReturn(Optional.of(batch));

        EvaluationBatchResponse response = evaluationBatchService.cancelEvaluationBatch(1L, 500L);

        assertThat(response.status()).isEqualTo(EvaluationBatchStatus.CANCEL_REQUESTED);
        assertThat(response.cancelRequestedAt()).isNotNull();
        assertThat(response.completedAt()).isNull();
    }

    @Test
    void getEvaluationBatchShouldThrowWhenMissing() {
        AiWorkflow workflow = new AiWorkflow(
                "Shipping Support Evaluation",
                "Evaluates shipping status extraction."
        );
        workflow.setId(1L);

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(evaluationBatchRepository.findByIdAndWorkflowId(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluationBatchService.getEvaluationBatch(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evaluation batch not found with id: 999");
    }
}