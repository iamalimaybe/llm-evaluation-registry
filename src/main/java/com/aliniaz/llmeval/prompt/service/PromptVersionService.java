package com.aliniaz.llmeval.prompt.service;

import com.aliniaz.llmeval.prompt.api.request.CreatePromptVersionRequest;
import com.aliniaz.llmeval.prompt.api.response.PromptVersionResponse;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;

import java.util.List;

public interface PromptVersionService {

    PromptVersionResponse createPromptVersion(Long workflowId, CreatePromptVersionRequest request);

    List<PromptVersionResponse> getPromptVersions(Long workflowId);

    PromptVersionResponse getPromptVersion(Long workflowId, Long promptVersionId);

    PromptVersion getPromptVersionEntity(Long workflowId, Long promptVersionId);
}