package com.aliniaz.llmeval.prompt.api;

import com.aliniaz.llmeval.prompt.api.request.CreatePromptVersionRequest;
import com.aliniaz.llmeval.prompt.api.response.PromptVersionResponse;
import com.aliniaz.llmeval.prompt.service.PromptVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PromptVersionController {

    private final PromptVersionService promptVersionService;

    @PostMapping("/api/workflows/{workflowId}/prompt-versions")
    @ResponseStatus(HttpStatus.CREATED)
    public PromptVersionResponse createPromptVersion(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreatePromptVersionRequest request
    ) {
        return promptVersionService.createPromptVersion(workflowId, request);
    }

    @GetMapping("/api/workflows/{workflowId}/prompt-versions")
    public List<PromptVersionResponse> getPromptVersions(@PathVariable Long workflowId) {
        return promptVersionService.getPromptVersions(workflowId);
    }

    @GetMapping("/api/workflows/{workflowId}/prompt-versions/{promptVersionId}")
    public PromptVersionResponse getPromptVersion(
            @PathVariable Long workflowId,
            @PathVariable Long promptVersionId
    ) {
        return promptVersionService.getPromptVersion(workflowId, promptVersionId);
    }
}