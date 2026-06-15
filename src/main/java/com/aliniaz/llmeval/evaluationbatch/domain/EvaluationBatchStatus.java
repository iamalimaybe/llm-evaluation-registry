package com.aliniaz.llmeval.evaluationbatch.domain;

public enum EvaluationBatchStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    COMPLETED_WITH_FAILURES,
    CANCEL_REQUESTED,
    CANCELLED,
    ERROR
}