package com.aliniaz.llmeval.workflow.api;

import com.aliniaz.llmeval.workflow.api.request.CreateWorkflowRequest;
import com.aliniaz.llmeval.workflow.api.response.WorkflowResponse;
import com.aliniaz.llmeval.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        return workflowService.createWorkflow(request);
    }

    @GetMapping
    public List<WorkflowResponse> getWorkflows() {
        return workflowService.getWorkflows();
    }

    @GetMapping("/{id}")
    public WorkflowResponse getWorkflow(@PathVariable Long id) {
        return workflowService.getWorkflow(id);
    }
}