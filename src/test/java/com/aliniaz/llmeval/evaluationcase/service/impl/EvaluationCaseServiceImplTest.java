package com.aliniaz.llmeval.evaluationcase.service.impl;

import com.aliniaz.llmeval.common.exception.DuplicateResourceException;
import com.aliniaz.llmeval.common.exception.ResourceNotFoundException;
import com.aliniaz.llmeval.evaluationcase.api.request.CreateEvaluationCaseRequest;
import com.aliniaz.llmeval.evaluationcase.api.response.EvaluationCaseResponse;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCaseRepository;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationCaseServiceImplTest {

    @Mock
    private EvaluationCaseRepository evaluationCaseRepository;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private EvaluationCaseServiceImpl evaluationCaseService;

    @Test
    void createEvaluationCaseShouldSaveEvaluationCaseWhenNameIsUnique() {
        AiWorkflow workflow = new AiWorkflow(
                "RAG Document Assistant",
                "Evaluates grounded document question answering workflows"
        );
        workflow.setId(1L);

        CreateEvaluationCaseRequest request = new CreateEvaluationCaseRequest(
                "Grounded answer rejects missing information",
                "RAG_QA",
                Map.of(
                        "question", "What is the refund policy?",
                        "context", "The document only contains shipping information."
                ),
                Map.of("answerType", "INSUFFICIENT_INFORMATION"),
                List.of("The document only contains shipping information."),
                List.of("The refund policy allows returns within 30 days."),
                Map.of("requiredFactsWeight", 60, "forbiddenClaimsWeight", 40)
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(evaluationCaseRepository.existsByWorkflowIdAndNameIgnoreCase(1L, request.name()))
                .thenReturn(false);
        when(evaluationCaseRepository.save(any(EvaluationCase.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvaluationCaseResponse response = evaluationCaseService.createEvaluationCase(1L, request);

        assertThat(response.workflowId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Grounded answer rejects missing information");
        assertThat(response.taskType()).isEqualTo("RAG_QA");
        assertThat(response.enabled()).isTrue();
        assertThat(response.requiredFacts()).containsExactly("The document only contains shipping information.");
        assertThat(response.forbiddenClaims()).containsExactly("The refund policy allows returns within 30 days.");

        verify(workflowService).getWorkflowEntity(1L);
        verify(evaluationCaseRepository).existsByWorkflowIdAndNameIgnoreCase(1L, request.name());
        verify(evaluationCaseRepository).save(any(EvaluationCase.class));
    }

    @Test
    void createEvaluationCaseShouldThrowDuplicateResourceExceptionWhenNameAlreadyExists() {
        AiWorkflow workflow = new AiWorkflow(
                "RAG Document Assistant",
                "Evaluates grounded document question answering workflows"
        );
        workflow.setId(1L);

        CreateEvaluationCaseRequest request = new CreateEvaluationCaseRequest(
                "Grounded answer rejects missing information",
                "RAG_QA",
                Map.of("question", "What is the refund policy?"),
                null,
                null,
                null,
                null
        );

        when(workflowService.getWorkflowEntity(1L)).thenReturn(workflow);
        when(evaluationCaseRepository.existsByWorkflowIdAndNameIgnoreCase(1L, request.name()))
                .thenReturn(true);

        assertThatThrownBy(() -> evaluationCaseService.createEvaluationCase(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Evaluation case already exists for workflow id 1: Grounded answer rejects missing information");

        verify(workflowService).getWorkflowEntity(1L);
        verify(evaluationCaseRepository).existsByWorkflowIdAndNameIgnoreCase(1L, request.name());
    }

    @Test
    void getEvaluationCaseShouldReturnEvaluationCaseWhenFound() {
        AiWorkflow workflow = new AiWorkflow(
                "AI Product Recommendation Service",
                "Evaluates recommendation quality against product facts"
        );
        workflow.setId(2L);

        EvaluationCase evaluationCase = new EvaluationCase(
                workflow,
                "Recommend based on explicit constraints",
                "PRODUCT_RECOMMENDATION",
                Map.of(
                        "userNeed", "I need a budget laptop for Java development.",
                        "budget", "under 250000 PKR"
                ),
                Map.of("category", "LAPTOP"),
                List.of("User needs a laptop for Java development."),
                List.of("Recommend a gaming desktop."),
                Map.of("minimumScore", 80)
        );
        evaluationCase.setId(10L);

        when(workflowService.getWorkflowEntity(2L)).thenReturn(workflow);
        when(evaluationCaseRepository.findByIdAndWorkflowId(10L, 2L))
                .thenReturn(Optional.of(evaluationCase));

        EvaluationCaseResponse response = evaluationCaseService.getEvaluationCase(2L, 10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.workflowId()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("Recommend based on explicit constraints");
        assertThat(response.taskType()).isEqualTo("PRODUCT_RECOMMENDATION");

        verify(workflowService).getWorkflowEntity(2L);
        verify(evaluationCaseRepository).findByIdAndWorkflowId(10L, 2L);
    }

    @Test
    void getEvaluationCaseShouldThrowResourceNotFoundExceptionWhenMissing() {
        AiWorkflow workflow = new AiWorkflow(
                "n8n Business Workflow Automation System",
                "Evaluates AI-assisted workflow automation behavior"
        );
        workflow.setId(3L);

        when(workflowService.getWorkflowEntity(3L)).thenReturn(workflow);
        when(evaluationCaseRepository.findByIdAndWorkflowId(99L, 3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> evaluationCaseService.getEvaluationCase(3L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Evaluation case not found with id: 99");

        verify(workflowService).getWorkflowEntity(3L);
        verify(evaluationCaseRepository).findByIdAndWorkflowId(99L, 3L);
    }
}