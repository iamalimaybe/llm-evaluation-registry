package com.aliniaz.llmeval.modelexecution.service;

import java.util.Map;

public interface ModelOutputParser {

    Map<String, Object> parse(String rawOutput);
}