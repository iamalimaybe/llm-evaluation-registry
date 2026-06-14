package com.aliniaz.llmeval.evaluationrun.service.impl;

import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRun;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationResult;
import com.aliniaz.llmeval.evaluationrun.service.EvaluationRunEvaluator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SimpleEvaluationRunEvaluator implements EvaluationRunEvaluator {

    private final ObjectMapper objectMapper;

    @Override
    public EvaluationResult evaluate(EvaluationRun evaluationRun) {
        Map<String, Object> parsedOutput = evaluationRun.getParsedOutput();
        EvaluationCase evaluationCase = evaluationRun.getEvaluationCase();

        if (parsedOutput == null || parsedOutput.isEmpty()) {
            return failed("Parsed output is empty.");
        }

        List<String> failureReasons = new ArrayList<>();

        List<String> criticalExpectedFields = criticalExpectedFields(evaluationCase.getScoringRules());
        boolean criticalExpectedFieldFailed = false;

        int totalChecks = 0;
        int passedChecks = 0;

        Map<String, Object> expectedOutput = evaluationCase.getExpectedOutput();

        if (expectedOutput != null && !expectedOutput.isEmpty()) {
            for (Map.Entry<String, Object> expectedEntry : expectedOutput.entrySet()) {
                totalChecks++;

                Object actualValue = parsedOutput.get(expectedEntry.getKey());

                if (Objects.equals(expectedEntry.getValue(), actualValue)) {
                    passedChecks++;
                } else {
                    failureReasons.add(
                            "Expected output field '%s' to be '%s' but was '%s'.".formatted(
                                    expectedEntry.getKey(),
                                    expectedEntry.getValue(),
                                    actualValue
                            )
                    );

                    if (criticalExpectedFields.contains(expectedEntry.getKey())) {
                        criticalExpectedFieldFailed = true;
                        failureReasons.add("Critical expected output field failed: " + expectedEntry.getKey());
                    }
                }
            }
        }

        String parsedOutputText = normalize(toJsonText(parsedOutput));

        List<String> requiredFacts = evaluationCase.getRequiredFacts();

        if (requiredFacts != null) {
            for (String requiredFact : requiredFacts) {
                if (requiredFact == null || requiredFact.isBlank()) {
                    continue;
                }

                totalChecks++;

                if (parsedOutputText.contains(normalize(requiredFact))) {
                    passedChecks++;
                } else {
                    failureReasons.add("Missing required fact: " + requiredFact);
                }
            }
        }

        List<String> forbiddenClaims = evaluationCase.getForbiddenClaims();

        if (forbiddenClaims != null) {
            for (String forbiddenClaim : forbiddenClaims) {
                if (forbiddenClaim == null || forbiddenClaim.isBlank()) {
                    continue;
                }

                totalChecks++;

                if (parsedOutputText.contains(normalize(forbiddenClaim))) {
                    failureReasons.add("Output contains forbidden claim: " + forbiddenClaim);
                } else {
                    passedChecks++;
                }
            }
        }

        if (totalChecks == 0) {
            return failed("No evaluation checks are configured for this evaluation case.");
        }

        if (criticalExpectedFieldFailed) {
            return new EvaluationResult(
                    false,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    failureReasons
            );
        }

        BigDecimal score = BigDecimal.valueOf(passedChecks)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalChecks), 2, RoundingMode.HALF_UP);

        return new EvaluationResult(
                failureReasons.isEmpty(),
                score,
                failureReasons
        );
    }

    private EvaluationResult failed(String reason) {
        return new EvaluationResult(
                false,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                List.of(reason)
        );
    }

    private String toJsonText(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private List<String> criticalExpectedFields(Map<String, Object> scoringRules) {
        if (scoringRules == null || scoringRules.isEmpty()) {
            return List.of();
        }

        Object value = scoringRules.get("criticalExpectedFields");

        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        List<String> fields = new ArrayList<>();

        for (Object item : values) {
            if (item instanceof String field && !field.isBlank()) {
                fields.add(field.trim());
            }
        }

        return fields;
    }
}