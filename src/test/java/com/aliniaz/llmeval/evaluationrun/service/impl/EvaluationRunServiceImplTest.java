package com.aliniaz.llmeval.evaluationrun.service.impl;

import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.service.EvaluationCaseService;
import com.aliniaz.llmeval.evaluationrun.api.request.CompleteEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.request.CreateEvaluationRunRequest;
import com.aliniaz.llmeval.evaluationrun.api.response.EvaluationRunResponse;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunRepository;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunStatus;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationResult;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunEvaluator;
import com.aliniaz.llmeval.modelexecution.service.*;
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
class EvaluationRunServiceImplTest {

    @Mock
    private EvaluationRunRepository evaluationRunRepository;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private PromptVersionService promptVersionService;

    @Mock
    private EvaluationCaseService evaluationCaseService;

    @Mock
    private ModelExecutionClientRouter modelExecutionClientRouter;

    @Mock
    private ModelExecutionClient modelExecutionClient;

    @Mock
    private PromptExecutionBuilder promptExecutionBuilder;

    @Mock
    private ModelOutputParser modelOutputParser;

    @Mock
    private EvaluationRunEvaluator evaluationRunEvaluator;

    @InjectMocks
    private EvaluationRunServiceImpl evaluationRunService;

    @Test
    void createEvaluationRunShouldSavePendingRunWithDefaultProvider() {
        AiWorkflow workflow = new AiWorkflow(
                "RAG Document Assistant",
                "Evaluates grounded document question answering workflows"
        );
        workflow.setId(1L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Answer only from the provided context.",
                "Initial grounded prompt"
        );
        promptVersion.setId(10L);

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Reject missing refund policy",
                "RAG_QA",
                Map.of("question", "What is the refund policy?"),
                Map.of("answerType", "INSUFFICIENT_INFORMATION"),
                null,
                null,
                null
        );
        evaluationCase.setId(20L);

        CreateEvaluationRunRequest request = new CreateEvaluationRunRequest(
                10L,
                20L,
                "qwen3:4b",
                null,
                null,
                new BigDecimal("0.20"),
                Map.of("contextWindow", 4096, "numPredict", 512)
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(promptVersionService.getPromptVersionEntity(1L, 10L)).thenReturn(promptVersion);
        when(evaluationCaseService.getEvaluationCaseEntity(1L, 20L)).thenReturn(evaluationCase);
        when(evaluationRunRepository.save(any(EvaluationRun.class)))
                .thenAnswer(invocation -> {
                    EvaluationRun run = invocation.getArgument(0);
                    run.setId(100L);
                    return run;
                });

        EvaluationRunResponse response = evaluationRunService.createEvaluationRun(1L, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.workflowId()).isEqualTo(1L);
        assertThat(response.promptVersionId()).isEqualTo(10L);
        assertThat(response.evaluationCaseId()).isEqualTo(20L);
        assertThat(response.modelName()).isEqualTo("qwen3:4b");
        assertThat(response.provider()).isEqualTo(EvaluationRunProvider.OLLAMA);
        assertThat(response.temperature()).isEqualByComparingTo("0.20");
        assertThat(response.runConfig()).containsEntry("contextWindow", 4096);
        assertThat(response.status()).isEqualTo(EvaluationRunStatus.PENDING);
        assertThat(response.passed()).isNull();
        assertThat(response.score()).isNull();

        verify(workflowService).getWorkflowEntity(1L);
        verify(promptVersionService).getPromptVersionEntity(1L, 10L);
        verify(evaluationCaseService).getEvaluationCaseEntity(1L, 20L);
        verify(evaluationRunRepository).save(any(EvaluationRun.class));
    }

    @Test
    void createEvaluationRunShouldUseProvidedProvider() {
        AiWorkflow workflow = new AiWorkflow(
                "Manual Evaluation Workflow",
                "Stores manually reviewed evaluation runs"
        );
        workflow.setId(1L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Manual prompt",
                "Initial manual prompt"
        );
        promptVersion.setId(10L);

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Manual review case",
                "MANUAL_REVIEW",
                Map.of("input", "Review this output."),
                null,
                null,
                null,
                null
        );
        evaluationCase.setId(20L);

        CreateEvaluationRunRequest request = new CreateEvaluationRunRequest(
                10L,
                20L,
                "manual-baseline",
                "v1",
                EvaluationRunProvider.MANUAL,
                null,
                null
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(promptVersionService.getPromptVersionEntity(1L, 10L)).thenReturn(promptVersion);
        when(evaluationCaseService.getEvaluationCaseEntity(1L, 20L)).thenReturn(evaluationCase);
        when(evaluationRunRepository.save(any(EvaluationRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationRunResponse response = evaluationRunService.createEvaluationRun(1L, request);

        assertThat(response.provider()).isEqualTo(EvaluationRunProvider.MANUAL);
        assertThat(response.modelName()).isEqualTo("manual-baseline");
        assertThat(response.modelVersion()).isEqualTo("v1");

        verify(evaluationRunRepository).save(any(EvaluationRun.class));
    }

    @Test
    void getEvaluationRunShouldReturnRunWhenFound() {
        AiWorkflow workflow = new AiWorkflow(
                "AI Product Recommendation Service",
                "Evaluates recommendation quality against product facts"
        );
        workflow.setId(2L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Recommend using catalog facts only.",
                "Initial recommendation prompt"
        );
        promptVersion.setId(30L);

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Recommend based on explicit constraints",
                "PRODUCT_RECOMMENDATION",
                Map.of("userNeed", "Budget laptop for Java development"),
                null,
                null,
                null,
                null
        );
        evaluationCase.setId(40L);

        EvaluationRun evaluationRun = new EvaluationRun(
                workflow,
                promptVersion,
                evaluationCase,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                new BigDecimal("0.10"),
                Map.of("seed", 123)
        );
        evaluationRun.setId(200L);

        when(workflowService.getWorkflowEntity(2L)).thenReturn(workflow);
        when(evaluationRunRepository.findByIdAndWorkflowId(200L, 2L))
                .thenReturn(Optional.of(evaluationRun));

        EvaluationRunResponse response = evaluationRunService.getEvaluationRun(2L, 200L);

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.workflowId()).isEqualTo(2L);
        assertThat(response.promptVersionId()).isEqualTo(30L);
        assertThat(response.evaluationCaseId()).isEqualTo(40L);
        assertThat(response.modelName()).isEqualTo("qwen3:4b");

        verify(workflowService).getWorkflowEntity(2L);
        verify(evaluationRunRepository).findByIdAndWorkflowId(200L, 2L);
    }

    @Test
    void getEvaluationRunShouldThrowResourceNotFoundExceptionWhenMissing() {
        AiWorkflow workflow = new AiWorkflow(
                "n8n Business Workflow Automation System",
                "Evaluates AI-assisted workflow automation behavior"
        );
        workflow.setId(3L);

        when(workflowService.getWorkflowEntity(3L)).thenReturn(workflow);
        when(evaluationRunRepository.findByIdAndWorkflowId(999L, 3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluationRunService.getEvaluationRun(3L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evaluation run not found with id: 999");

        verify(workflowService).getWorkflowEntity(3L);
        verify(evaluationRunRepository).findByIdAndWorkflowId(999L, 3L);
    }

    @Test
    void completeEvaluationRunShouldMarkRunAsPassedWhenResultPasses() {
        AiWorkflow workflow = new AiWorkflow(
                "RAG Document Assistant",
                "Evaluates grounded document question answering workflows"
        );
        workflow.setId(1L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Answer only from the provided context.",
                "Initial grounded prompt"
        );
        promptVersion.setId(10L);

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Reject missing refund policy",
                "RAG_QA",
                Map.of("question", "What is the refund policy?"),
                Map.of("answerType", "INSUFFICIENT_INFORMATION"),
                null,
                null,
                null
        );
        evaluationCase.setId(20L);

        EvaluationRun evaluationRun = new EvaluationRun(
                workflow,
                promptVersion,
                evaluationCase,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                new BigDecimal("0.20"),
                Map.of("contextWindow", 4096)
        );
        evaluationRun.setId(100L);

        CompleteEvaluationRunRequest request = new CompleteEvaluationRunRequest(
                "{\"answerType\":\"INSUFFICIENT_INFORMATION\"}",
                Map.of("answerType", "INSUFFICIENT_INFORMATION"),
                new BigDecimal("88.50"),
                true,
                new BigDecimal("95.00"),
                List.of(),
                "Output correctly avoided unsupported refund claim."
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(evaluationRunRepository.findByIdAndWorkflowId(100L, 1L))
                .thenReturn(Optional.of(evaluationRun));

        EvaluationRunResponse response = evaluationRunService.completeEvaluationRun(1L, 100L, request);

        assertThat(response.status()).isEqualTo(EvaluationRunStatus.PASSED);
        assertThat(response.passed()).isTrue();
        assertThat(response.score()).isEqualByComparingTo("95.00");
        assertThat(response.confidence()).isEqualByComparingTo("88.50");
        assertThat(response.rawOutput()).isEqualTo("{\"answerType\":\"INSUFFICIENT_INFORMATION\"}");
        assertThat(response.parsedOutput()).containsEntry("answerType", "INSUFFICIENT_INFORMATION");
        assertThat(response.failureReasons()).isEmpty();
        assertThat(response.reviewerNotes()).isEqualTo("Output correctly avoided unsupported refund claim.");
        assertThat(response.completedAt()).isNotNull();

        verify(workflowService).getWorkflowEntity(1L);
        verify(evaluationRunRepository).findByIdAndWorkflowId(100L, 1L);
    }

    @Test
    void completeEvaluationRunShouldMarkRunAsFailedWhenResultFails() {
        AiWorkflow workflow = new AiWorkflow(
                "AI Product Recommendation Service",
                "Evaluates recommendation quality against product facts"
        );
        workflow.setId(2L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Recommend using catalog facts only.",
                "Initial recommendation prompt"
        );
        promptVersion.setId(30L);

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Avoid unsupported product claim",
                "PRODUCT_RECOMMENDATION",
                Map.of("userNeed", "Budget laptop for Java development"),
                null,
                null,
                List.of("Claims the laptop has a dedicated GPU when catalog does not say that."),
                null
        );
        evaluationCase.setId(40L);

        EvaluationRun evaluationRun = new EvaluationRun(
                workflow,
                promptVersion,
                evaluationCase,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                new BigDecimal("0.20"),
                Map.of("contextWindow", 4096)
        );
        evaluationRun.setId(200L);

        CompleteEvaluationRunRequest request = new CompleteEvaluationRunRequest(
                "{\"recommendation\":\"Laptop A has a dedicated GPU.\"}",
                Map.of("recommendation", "Laptop A has a dedicated GPU."),
                new BigDecimal("72.00"),
                false,
                new BigDecimal("45.00"),
                List.of("Output included an unsupported dedicated GPU claim."),
                "Needs prompt correction to avoid inventing product specs."
        );

        when(workflowService.getWorkflowEntity(2L)).thenReturn(workflow);
        when(evaluationRunRepository.findByIdAndWorkflowId(200L, 2L))
                .thenReturn(Optional.of(evaluationRun));

        EvaluationRunResponse response = evaluationRunService.completeEvaluationRun(2L, 200L, request);

        assertThat(response.status()).isEqualTo(EvaluationRunStatus.FAILED);
        assertThat(response.passed()).isFalse();
        assertThat(response.score()).isEqualByComparingTo("45.00");
        assertThat(response.failureReasons()).containsExactly("Output included an unsupported dedicated GPU claim.");
        assertThat(response.reviewerNotes()).isEqualTo("Needs prompt correction to avoid inventing product specs.");
        assertThat(response.completedAt()).isNotNull();

        verify(workflowService).getWorkflowEntity(2L);
        verify(evaluationRunRepository).findByIdAndWorkflowId(200L, 2L);
    }

    @Test
    void executeEvaluationRunShouldCaptureParseAndEvaluateModelOutput() {
        AiWorkflow workflow = new AiWorkflow(
                "Shipping Status Evaluation",
                "Evaluates whether model output avoids unsupported shipping claims"
        );
        workflow.setId(2L);

        PromptVersion promptVersion = new PromptVersion(
                workflow,
                "v1",
                "Extract supported facts only.",
                "Initial extraction prompt"
        );
        promptVersion.setId(2L);

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Missing shipping status",
                "EXTRACTION",
                Map.of("message", "The customer asked about shipping status."),
                Map.of("status", "INSUFFICIENT_INFORMATION"),
                List.of("Shipping status is not specified"),
                List.of("shipped", "delivered"),
                null
        );
        evaluationCase.setId(2L);

        EvaluationRun evaluationRun = new EvaluationRun(
                workflow,
                promptVersion,
                evaluationCase,
                "qwen3:4b",
                null,
                EvaluationRunProvider.OLLAMA,
                BigDecimal.ZERO,
                Map.of("contextWindow", 4096, "numPredict", 512)
        );
        evaluationRun.setId(14L);

        String controlledPrompt = "Controlled execution prompt";

        String rawOutput = """
            {
              "context": "The customer asked about shipping status.",
              "instruction": "Extract supported facts only."
            }
            """;

        Map<String, Object> parsedOutput = Map.of(
                "context", "The customer asked about shipping status.",
                "instruction", "Extract supported facts only."
        );

        EvaluationResult evaluationResult = new EvaluationResult(
                false,
                new BigDecimal("66.67"),
                List.of("Expected output field 'status' to be 'INSUFFICIENT_INFORMATION' but was 'null'.")
        );

        when(workflowService.getWorkflowEntity(2L)).thenReturn(workflow);
        when(evaluationRunRepository.findByIdAndWorkflowId(14L, 2L))
                .thenReturn(Optional.of(evaluationRun));
        when(promptExecutionBuilder.buildPrompt(evaluationRun)).thenReturn(controlledPrompt);
        when(modelExecutionClientRouter.getClient(any(ModelExecutionRequest.class)))
                .thenReturn(modelExecutionClient);
        when(modelExecutionClient.execute(any(ModelExecutionRequest.class)))
                .thenReturn(new ModelExecutionResponse(
                        rawOutput,
                        Map.of("provider", "OLLAMA", "model", "qwen3:4b", "format", "json")
                ));
        when(modelOutputParser.parse(rawOutput)).thenReturn(parsedOutput);
        when(evaluationRunEvaluator.evaluate(evaluationRun)).thenReturn(evaluationResult);

        EvaluationRunResponse response = evaluationRunService.executeEvaluationRun(2L, 14L);

        assertThat(response.status()).isEqualTo(EvaluationRunStatus.FAILED);
        assertThat(response.rawOutput()).isEqualTo(rawOutput);
        assertThat(response.parsedOutput()).containsEntry(
                "context",
                "The customer asked about shipping status."
        );
        assertThat(response.passed()).isFalse();
        assertThat(response.score()).isEqualByComparingTo("66.67");
        assertThat(response.failureReasons()).containsExactly(
                "Expected output field 'status' to be 'INSUFFICIENT_INFORMATION' but was 'null'."
        );
        assertThat(response.startedAt()).isNotNull();
        assertThat(response.completedAt()).isNotNull();

        verify(workflowService).getWorkflowEntity(2L);
        verify(evaluationRunRepository).findByIdAndWorkflowId(14L, 2L);
        verify(promptExecutionBuilder).buildPrompt(evaluationRun);
        verify(modelExecutionClientRouter).getClient(any(ModelExecutionRequest.class));
        verify(modelExecutionClient).execute(any(ModelExecutionRequest.class));
        verify(modelOutputParser).parse(rawOutput);
        verify(evaluationRunEvaluator).evaluate(evaluationRun);
    }
}