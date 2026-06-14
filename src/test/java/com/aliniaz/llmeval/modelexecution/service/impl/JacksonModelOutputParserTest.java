package com.aliniaz.llmeval.modelexecution.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonModelOutputParserTest {

    private final JacksonModelOutputParser parser = new JacksonModelOutputParser(
            new ObjectMapper()
    );

    @Test
    void shouldParseValidTopLevelJsonObject() {
        String rawOutput = """
                {
                  "status": "INSUFFICIENT_INFORMATION",
                  "reason": "Shipping status is not specified."
                }
                """;

        Map<String, Object> parsedOutput = parser.parse(rawOutput);

        assertThat(parsedOutput).containsEntry("status", "INSUFFICIENT_INFORMATION");
        assertThat(parsedOutput).containsEntry("reason", "Shipping status is not specified.");
    }

    @Test
    void shouldRejectBlankOutput() {
        assertThatThrownBy(() -> parser.parse(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Model output is blank and cannot be parsed.");
    }

    @Test
    void shouldRejectInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Model output is not a valid top-level JSON object.");
    }

    @Test
    void shouldRejectJsonArrayBecauseOutputMustBeObject() {
        assertThatThrownBy(() -> parser.parse("[{\"status\":\"OK\"}]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Model output is not a valid top-level JSON object.");
    }
}