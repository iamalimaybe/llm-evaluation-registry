## MVP Scope

LLM Evaluation Registry is a backend-led quality layer for AI workflows.

It helps track and compare LLM behavior across workflows, prompt versions, evaluation cases, model runs, manual result recording, and regression checks. The goal is to make AI behavior measurable and reviewable instead of relying on informal judgment.

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
