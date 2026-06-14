## MVP Scope

LLM Evaluation Registry is a backend-led quality layer for AI workflows.

It helps track and compare LLM behavior across workflows, prompt versions, evaluation cases, model runs, manual result recording, and regression checks. The goal is to make AI behavior measurable and reviewable instead of relying on informal judgment.

## API Usage

The API is available locally at:

```text
http://localhost:8080
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

### Main API Flow

A typical evaluation registry flow is:

1. Create a workflow
2. Add a prompt version for that workflow
3. Add an evaluation case
4. Create an evaluation run
5. Record the result for the run
6. Compare a baseline run against a candidate run

### Current Endpoints

```text
POST  /api/workflows
GET   /api/workflows
GET   /api/workflows/{id}

POST  /api/workflows/{workflowId}/prompt-versions
GET   /api/workflows/{workflowId}/prompt-versions
GET   /api/workflows/{workflowId}/prompt-versions/{promptVersionId}

POST  /api/workflows/{workflowId}/evaluation-cases
GET   /api/workflows/{workflowId}/evaluation-cases
GET   /api/workflows/{workflowId}/evaluation-cases/{evaluationCaseId}

POST  /api/workflows/{workflowId}/evaluation-runs
GET   /api/workflows/{workflowId}/evaluation-runs
GET   /api/workflows/{workflowId}/evaluation-runs/{evaluationRunId}
PATCH /api/workflows/{workflowId}/evaluation-runs/{evaluationRunId}/result

GET   /api/workflows/{workflowId}/evaluation-runs/compare?baselineRunId={baselineRunId}&candidateRunId={candidateRunId}
```

### Regression Comparison Example

The comparison endpoint compares a baseline evaluation run against a candidate run.

Example:

```text
GET /api/workflows/2/evaluation-runs/compare?baselineRunId=4&candidateRunId=5
```

Example response:

```json
{
  "workflowId": 2,
  "baselineRunId": 4,
  "candidateRunId": 5,
  "baselineScore": 0.92,
  "candidateScore": 0.60,
  "scoreDelta": -0.32,
  "baselinePassed": true,
  "candidatePassed": false,
  "outcome": "REGRESSED",
  "regressionReasons": [
    "Candidate failed while baseline passed.",
    "Candidate score is lower than baseline score."
  ]
}
```

### Current Features

* Create and list AI workflows
* Create and list prompt versions for a workflow
* Create and list reusable evaluation cases
* Create evaluation runs with model and runtime metadata
* Store raw model output and parsed structured output
* Record manual pass/fail results, scores, confidence, failure reasons, and reviewer notes
* Compare a baseline evaluation run against a candidate run
* Detect simple regression outcomes such as `IMPROVED`, `REGRESSED`, `UNCHANGED`, and `NOT_COMPARABLE`

### Current Design

The backend is built with Java 17, Spring Boot 3, PostgreSQL, Spring Data JPA, and Liquibase.

The system is intentionally backend-first. It does not call Ollama yet. Evaluation runs can currently be created and completed manually, which keeps the registry model stable before adding automated model execution.

### Why This Matters

Prompt and model changes can silently make an AI workflow worse. This registry shows how an engineering team can track versions, store outputs, record evaluation results, compare runs, and identify regressions before trusting a changed AI workflow.
