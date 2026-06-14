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
5. Execute the run through a configured model provider, or record the result manually
6. Store raw output and parsed structured output
7. Evaluate the parsed output against expected output, required facts, and forbidden claims
8. Compare a baseline run against a candidate run

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
POST  /api/workflows/{workflowId}/evaluation-runs/{evaluationRunId}/execute
PATCH /api/workflows/{workflowId}/evaluation-runs/{evaluationRunId}/result

GET   /api/workflows/{workflowId}/evaluation-runs/compare?baselineRunId={baselineRunId}&candidateRunId={candidateRunId}
```
### Model Execution Example

The execution endpoint runs an existing evaluation run through the configured model provider.

Example:

```text
POST /api/workflows/2/evaluation-runs/14/execute
```

Example response:

```json
{
  "id": 14,
  "workflowId": 2,
  "promptVersionId": 2,
  "evaluationCaseId": 2,
  "modelName": "qwen3:4b",
  "provider": "OLLAMA",
  "rawOutput": "{\n  \"context\": \"The customer asked about shipping status.\",\n  \"instruction\": \"Extract supported facts only.\"\n}",
  "parsedOutput": {
    "context": "The customer asked about shipping status.",
    "instruction": "Extract supported facts only."
  },
  "status": "FAILED",
  "passed": false,
  "score": 66.67,
  "failureReasons": [
    "Expected output field 'status' to be 'INSUFFICIENT_INFORMATION' but was 'null'."
  ]
}
```

### Scoring Rules

Evaluation cases can define optional scoring rules to make specific checks critical.

Example:

```json
{
  "criticalExpectedFields": ["status"],
  "criticalRequiredFacts": ["Shipping status is not specified"],
  "criticalForbiddenClaims": ["delivered"]
}
```

Supported rules:

* `criticalExpectedFields`: if any listed expected output field is missing or wrong, the run fails with score `0.00`
* `criticalRequiredFacts`: if any listed required fact is missing from the parsed output, the run fails with score `0.00`
* `criticalForbiddenClaims`: if any listed forbidden claim appears in the parsed output, the run fails with score `0.00`

This keeps scoring from being misleading when a model passes minor checks but fails a critical requirement.

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
* Execute evaluation runs through Ollama
* Build controlled model execution prompts from prompt versions and evaluation case input
* Store raw model output
* Parse valid JSON model output into structured `parsedOutput`
* Evaluate parsed output against expected output, required facts, and forbidden claims
* Store pass/fail result, score, and failure reasons
* Record manual pass/fail results, scores, confidence, failure reasons, and reviewer notes
* Compare a baseline evaluation run against a candidate run
* Detect simple regression outcomes such as `IMPROVED`, `REGRESSED`, `UNCHANGED`, and `NOT_COMPARABLE`
* Configure critical scoring rules for expected fields, required facts, and forbidden claims

### Current Design

The backend is built with Java 17, Spring Boot 3, PostgreSQL, Spring Data JPA, Liquibase, and Ollama.

The system is intentionally backend-first. It can create registry data, execute local model runs through Ollama, capture raw output, parse JSON output, and apply deterministic evaluation checks.

The evaluator is intentionally simple at this stage. It checks expected output fields, required facts, and forbidden claims. It is not yet a full semantic evaluator.

### Why This Matters

Prompt and model changes can silently make an AI workflow worse. This registry shows how an engineering team can track versions, store outputs, record evaluation results, compare runs, and identify regressions before trusting a changed AI workflow.
