package com.aliniaz.llmeval.workflow.service;

import com.aliniaz.llmeval.workflow.api.request.CreateWorkflowRequest;
import com.aliniaz.llmeval.workflow.api.response.WorkflowResponse;

import java.util.List;

public interface WorkflowService {

    WorkflowResponse createWorkflow(CreateWorkflowRequest request);

    List<WorkflowResponse> getWorkflows();

    WorkflowResponse getWorkflow(Long id);
}