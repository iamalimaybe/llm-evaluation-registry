package com.aliniaz.llmeval.modelexecution.service;

public interface ModelExecutionClientRouter {

    ModelExecutionClient getClient(ModelExecutionRequest request);
}