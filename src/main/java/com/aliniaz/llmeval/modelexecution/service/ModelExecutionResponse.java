package com.aliniaz.llmeval.modelexecution.service;

import java.util.Map;

public record ModelExecutionResponse(
        String rawOutput,
        Map<String, Object> metadata
) {
}