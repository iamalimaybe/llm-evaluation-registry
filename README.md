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

### Environment Configuration

Local configuration can be provided through environment variables.

```text
OLLAMA_BASE_URL=http://localhost:11434
OPENAI_API_KEY=
OPENAI_BASE_URL=https://api.openai.com
```

`OPENAI_API_KEY` is optional unless executing runs with `provider = OPENAI`.

The root `.env` file is local-only and should not be committed. Use `.env.example` as the committed reference.

### Main API Flow

A typical evaluation registry flow is:

1. Create a workflow
2. Add one or more prompt versions for that workflow
3. Add reusable evaluation cases
4. Create an evaluation run, or create an evaluation batch across all enabled cases
5. Execute runs through a configured model provider, or record results manually
6. Store raw output and parsed structured output
7. Evaluate parsed output against expected output, required facts, and forbidden claims
8. Compare a baseline run against a candidate run
9. Compare a baseline batch against a candidate batch

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

POST  /api/workflows/{workflowId}/evaluation-batches
GET   /api/workflows/{workflowId}/evaluation-batches
GET   /api/workflows/{workflowId}/evaluation-batches/compare?baselineBatchId={baselineBatchId}&candidateBatchId={candidateBatchId}
GET   /api/workflows/{workflowId}/evaluation-batches/{batchId}
POST  /api/workflows/{workflowId}/evaluation-batches/{batchId}/cancel
GET   /api/workflows/{workflowId}/evaluation-batches/{batchId}/runs

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

### Model Providers

The registry supports provider-based model execution through a shared model execution abstraction.

Currently supported providers:

```text
OLLAMA
OPENAI
MANUAL
```

`OLLAMA` is used for local model execution through a locally running Ollama server.

`OPENAI` is used for remote model execution through the OpenAI Responses API. It requires an API key with active API billing/quota.

Example OpenAI run request:

```json
{
    "promptVersionId": 10,
    "evaluationCaseId": 24,
    "modelName": "gpt-4.1-mini",
    "provider": "OPENAI",
    "temperature": 0,
    "runConfig": {
      "numPredict": 128
    }
}
```

The same evaluation pipeline is used regardless of provider:

1. Build controlled execution prompt
2. Execute model through selected provider
3. Store raw model output
4. Parse JSON output
5. Evaluate parsed output against deterministic checks
6. Store pass/fail result, score, and failure reasons

OpenAI support is optional. Local development can still use Ollama without an OpenAI API key.

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

### Queued Batch Evaluation

Evaluation batches allow one prompt version and model configuration to be run across all enabled evaluation cases for a workflow.

Example:

```text
POST /api/workflows/{workflowId}/evaluation-batches
```

Example request:

```json
{
  "promptVersionId": 5,
  "modelName": "qwen3:4b",
  "provider": "OLLAMA",
  "temperature": 0.0,
  "runConfig": {
    "numPredict": 128,
    "contextWindow": 4096
  }
}
```

Batch execution is asynchronous. The API returns a queued batch immediately, then a single background worker processes queued batches one at a time.

Progress can be checked with:

```text
GET /api/workflows/{workflowId}/evaluation-batches/{batchId}
```

Runs created by the batch can be inspected with:

```text
GET /api/workflows/{workflowId}/evaluation-batches/{batchId}/runs
```

A batch can be cancelled with:

```text
POST /api/workflows/{workflowId}/evaluation-batches/{batchId}/cancel
```

Cancellation behavior:

* QUEUED batches become CANCELLED
* RUNNING batches become CANCEL_REQUESTED
* The active model call is allowed to finish
* The worker stops before starting the next evaluation case

This keeps local model execution safe and predictable while still supporting queued evaluation workflows.

### Batch Regression Comparison

Completed evaluation batches can be compared to detect whether a candidate prompt/model configuration improved, regressed, stayed unchanged, or is not comparable.

Example:

```text
GET /api/workflows/{workflowId}/evaluation-batches/compare?baselineBatchId={baselineBatchId}&candidateBatchId={candidateBatchId}
```

The comparison includes:

* batch status comparability
* total and completed run counts
* passed, failed, and errored run counts
* average score delta
* overall outcome
* per-evaluation-case comparison
* regression reasons where applicable

Possible outcomes:

```text
IMPROVED
REGRESSED
UNCHANGED
NOT_COMPARABLE
```

Example response:

```json
{
  "workflowId": 7,
  "baselineBatchId": 9,
  "candidateBatchId": 10,
  "baselineStatus": "COMPLETED_WITH_FAILURES",
  "candidateStatus": "COMPLETED_WITH_FAILURES",
  "baselineTotalRuns": 4,
  "candidateTotalRuns": 4,
  "baselineCompletedRuns": 4,
  "candidateCompletedRuns": 4,
  "baselinePassedRuns": 2,
  "candidatePassedRuns": 2,
  "baselineFailedRuns": 2,
  "candidateFailedRuns": 2,
  "baselineErroredRuns": 0,
  "candidateErroredRuns": 0,
  "baselineAverageScore": 50.00,
  "candidateAverageScore": 50.00,
  "scoreDelta": 0.00,
  "outcome": "UNCHANGED",
  "comparisonReasons": [
    "Candidate batch matched baseline score and run outcome counts."
  ],
  "caseComparisons": [
    {
      "evaluationCaseId": 20,
      "baselineRunId": 42,
      "candidateRunId": 46,
      "baselineStatus": "PASSED",
      "candidateStatus": "PASSED",
      "baselinePassed": true,
      "candidatePassed": true,
      "baselineScore": 100.00,
      "candidateScore": 100.00,
      "scoreDelta": 0.00,
      "outcome": "UNCHANGED",
      "regressionReasons": []
    }
  ]
}
```

This is intentionally deterministic. It does not perform semantic judgment. It compares persisted evaluation results, scores, statuses, and case-level outcomes.

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
* Execute evaluation runs through OpenAI when an API key with active billing/quota is configured
* Route model execution through provider-specific clients
* Capture provider execution failures as persisted evaluation run errors
* Build controlled model execution prompts from prompt versions and evaluation case input
* Store raw model output
* Parse valid JSON model output into structured `parsedOutput`
* Evaluate parsed output against expected output, required facts, and forbidden claims
* Store pass/fail result, score, and failure reasons
* Record manual pass/fail results, scores, confidence, failure reasons, and reviewer notes
* Compare a baseline evaluation run against a candidate run
* Compare a baseline evaluation batch against a candidate batch
* Report batch-level and per-case comparison outcomes
* Detect simple regression outcomes such as `IMPROVED`, `REGRESSED`, `UNCHANGED`, and `NOT_COMPARABLE`
* Configure critical scoring rules for expected fields, required facts, and forbidden claims
* Create queued evaluation batches across all enabled evaluation cases
* Process evaluation batches asynchronously with a single local worker
* Track batch progress, totals, pass/fail/error counts, and average score
* View evaluation runs created by a batch
* Cancel queued or running evaluation batches
* Use local PowerShell smoke scripts to verify batch execution, cancellation, and comparison flows

### Current Design

The backend is built with Java 17, Spring Boot 3, PostgreSQL, Spring Data JPA, Liquibase, and Ollama.

The system is intentionally backend-first. It can create registry data, execute local model runs through Ollama, capture raw output, parse JSON output, and apply deterministic evaluation checks.

The evaluator is intentionally simple at this stage. It checks expected output fields, required facts, and forbidden claims. It is not yet a full semantic evaluator.

### Manual Smoke Scripts

The `scratch/` folder contains PowerShell scripts used for local manual smoke testing.

```text
scratch/test-batch-flow.ps1
scratch/test-batch-comparison-flow.ps1
scratch/test-cancel-queued-batch.ps1
scratch/test-cancel-running-batch.ps1
scratch/test-openai-provider-flow.ps1
```
The OpenAI smoke script requires `OPENAI_API_KEY` and active API billing/quota. Without quota, the run is expected to persist an `ERROR` result with the provider error response.

These scripts are not a replacement for unit tests or CI. They are local verification helpers for exercising the API with a running Spring Boot app, PostgreSQL database, and Ollama model.

What they cover:

* creating workflows, prompt versions, and evaluation cases
* creating queued evaluation batches
* polling batch progress
* inspecting batch-created evaluation runs
* comparing baseline and candidate batches
* cancelling queued batches
* requesting cancellation for running batches
* executing a single evaluation run through the OpenAI provider

Some cancellation scripts may require local workflow and prompt version IDs to be adjusted before running.

### Why This Matters

Prompt and model changes can silently make an AI workflow worse. This registry shows how an engineering team can track versions, store outputs, record evaluation results, compare runs, and identify regressions before trusting a changed AI workflow.
