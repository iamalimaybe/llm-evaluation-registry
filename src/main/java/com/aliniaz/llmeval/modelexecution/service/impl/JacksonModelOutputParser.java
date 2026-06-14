package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.modelexecution.service.ModelOutputParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class JacksonModelOutputParser implements ModelOutputParser {

    private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> parse(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            throw new IllegalArgumentException("Model output is blank and cannot be parsed.");
        }

        try {
            return objectMapper.readValue(rawOutput, JSON_OBJECT_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Model output is not a valid top-level JSON object.", exception);
        }
    }
}