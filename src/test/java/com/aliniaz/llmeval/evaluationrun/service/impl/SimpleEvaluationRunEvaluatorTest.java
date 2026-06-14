package com.aliniaz.llmeval.evaluationrun.service.impl;

import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimpleEvaluationRunEvaluatorTest {

    private final SimpleEvaluationRunEvaluator evaluator = new SimpleEvaluationRunEvaluator(
            new ObjectMapper()
    );

    @Test
    void shouldPassWhenExpectedOutputAndRequiredFactsMatch() {
        EvaluationCase evaluationCase = mock(EvaluationCase.class);
        EvaluationRun evaluationRun = mock(EvaluationRun.class);

        when(evaluationRun.getParsedOutput()).thenReturn(Map.of(
                "status", "INSUFFICIENT_INFORMATION",
                "reason", "Shipping status is not specified in the context."
        ));
        when(evaluationRun.getEvaluationCase()).thenReturn(evaluationCase);

        when(evaluationCase.getExpectedOutput()).thenReturn(Map.of(
                "status", "INSUFFICIENT_INFORMATION"
        ));
        when(evaluationCase.getRequiredFacts()).thenReturn(List.of(
                "Shipping status is not specified"
        ));
        when(evaluationCase.getForbiddenClaims()).thenReturn(List.of(
                "shipped",
                "delivered"
        ));

        EvaluationResult result = evaluator.evaluate(evaluationRun);

        assertThat(result.passed()).isTrue();
        assertThat(result.score()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.failureReasons()).isEmpty();
    }

    @Test
    void shouldFailWhenExpectedOutputFieldIsMissing() {
        EvaluationCase evaluationCase = mock(EvaluationCase.class);
        EvaluationRun evaluationRun = mock(EvaluationRun.class);

        when(evaluationRun.getParsedOutput()).thenReturn(Map.of(
                "context", "The customer asked about shipping status.",
                "instruction", "Extract supported facts only."
        ));
        when(evaluationRun.getEvaluationCase()).thenReturn(evaluationCase);

        when(evaluationCase.getExpectedOutput()).thenReturn(Map.of(
                "status", "INSUFFICIENT_INFORMATION"
        ));
        when(evaluationCase.getRequiredFacts()).thenReturn(List.of());
        when(evaluationCase.getForbiddenClaims()).thenReturn(List.of());

        EvaluationResult result = evaluator.evaluate(evaluationRun);

        assertThat(result.passed()).isFalse();
        assertThat(result.score()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(result.failureReasons()).containsExactly(
                "Expected output field 'status' to be 'INSUFFICIENT_INFORMATION' but was 'null'."
        );
    }

    @Test
    void shouldFailWhenOutputContainsForbiddenClaim() {
        EvaluationCase evaluationCase = mock(EvaluationCase.class);
        EvaluationRun evaluationRun = mock(EvaluationRun.class);

        when(evaluationRun.getParsedOutput()).thenReturn(Map.of(
                "status", "SHIPPED",
                "reason", "The package has been delivered."
        ));
        when(evaluationRun.getEvaluationCase()).thenReturn(evaluationCase);

        when(evaluationCase.getExpectedOutput()).thenReturn(Map.of(
                "status", "SHIPPED"
        ));
        when(evaluationCase.getRequiredFacts()).thenReturn(List.of());
        when(evaluationCase.getForbiddenClaims()).thenReturn(List.of(
                "delivered"
        ));

        EvaluationResult result = evaluator.evaluate(evaluationRun);

        assertThat(result.passed()).isFalse();
        assertThat(result.score()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.failureReasons()).containsExactly(
                "Output contains forbidden claim: delivered"
        );
    }

    @Test
    void shouldFailWhenNoEvaluationChecksAreConfigured() {
        EvaluationCase evaluationCase = mock(EvaluationCase.class);
        EvaluationRun evaluationRun = mock(EvaluationRun.class);

        when(evaluationRun.getParsedOutput()).thenReturn(Map.of(
                "status", "INSUFFICIENT_INFORMATION"
        ));
        when(evaluationRun.getEvaluationCase()).thenReturn(evaluationCase);

        when(evaluationCase.getExpectedOutput()).thenReturn(null);
        when(evaluationCase.getRequiredFacts()).thenReturn(null);
        when(evaluationCase.getForbiddenClaims()).thenReturn(null);

        EvaluationResult result = evaluator.evaluate(evaluationRun);

        assertThat(result.passed()).isFalse();
        assertThat(result.score()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(result.failureReasons()).containsExactly(
                "No evaluation checks are configured for this evaluation case."
        );
    }
}