package com.aliniaz.llmeval.modelexecution.service.impl;

import com.aliniaz.llmeval.modelexecution.service.ModelExecutionClient;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionClientRouter;
import com.aliniaz.llmeval.modelexecution.service.ModelExecutionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelExecutionClientRouterImpl implements ModelExecutionClientRouter {

    private final List<ModelExecutionClient> clients;

    @Override
    public ModelExecutionClient getClient(ModelExecutionRequest request) {
        return clients.stream()
                .filter(client -> client.supports(request.provider()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No model execution client configured for provider: " + request.provider()
                ));
    }
}