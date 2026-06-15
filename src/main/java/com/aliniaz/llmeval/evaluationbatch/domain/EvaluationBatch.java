package com.aliniaz.llmeval.evaluationbatch.domain;

import com.aliniaz.llmeval.evaluationrun.domain.EvaluationRunProvider;
import com.aliniaz.llmeval.prompt.domain.PromptVersion;
import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "evaluation_batches")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_version_id", nullable = false)
    private PromptVersion promptVersion;

    @Column(nullable = false, length = 120)
    private String modelName;

    @Column(length = 120)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private EvaluationRunProvider provider = EvaluationRunProvider.OLLAMA;

    @Column(precision = 4, scale = 2)
    private BigDecimal temperature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> runConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EvaluationBatchStatus status = EvaluationBatchStatus.QUEUED;

    @Column(nullable = false)
    private int totalRuns;

    @Column(nullable = false)
    private int completedRuns;

    @Column(nullable = false)
    private int passedRuns;

    @Column(nullable = false)
    private int failedRuns;

    @Column(nullable = false)
    private int erroredRuns;

    @Column(precision = 5, scale = 2)
    private BigDecimal averageScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> failureReasons;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelRequestedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public EvaluationBatch(
            AiWorkflow workflow,
            PromptVersion promptVersion,
            String modelName,
            String modelVersion,
            EvaluationRunProvider provider,
            BigDecimal temperature,
            Map<String, Object> runConfig,
            int totalRuns
    ) {
        this.workflow = workflow;
        this.promptVersion = promptVersion;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.provider = provider;
        this.temperature = temperature;
        this.runConfig = runConfig;
        this.totalRuns = totalRuns;
        this.status = EvaluationBatchStatus.QUEUED;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markRunning() {
        LocalDateTime now = LocalDateTime.now();

        this.status = EvaluationBatchStatus.RUNNING;
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void requestCancel() {
        LocalDateTime now = LocalDateTime.now();

        if (this.status == EvaluationBatchStatus.QUEUED) {
            this.status = EvaluationBatchStatus.CANCELLED;
            this.completedAt = now;
        } else if (this.status == EvaluationBatchStatus.RUNNING) {
            this.status = EvaluationBatchStatus.CANCEL_REQUESTED;
            this.cancelRequestedAt = now;
        }

        this.updatedAt = now;
    }

    public boolean isCancellationRequested() {
        return this.status == EvaluationBatchStatus.CANCEL_REQUESTED;
    }

    public void markCancelled() {
        LocalDateTime now = LocalDateTime.now();

        this.status = EvaluationBatchStatus.CANCELLED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void recordProgress(
            int completedRuns,
            int passedRuns,
            int failedRuns,
            int erroredRuns,
            BigDecimal averageScore
    ) {
        this.completedRuns = completedRuns;
        this.passedRuns = passedRuns;
        this.failedRuns = failedRuns;
        this.erroredRuns = erroredRuns;
        this.averageScore = averageScore;
        this.updatedAt = LocalDateTime.now();
    }

    public void markCompleted() {
        LocalDateTime now = LocalDateTime.now();

        this.status = failedRuns > 0 || erroredRuns > 0
                ? EvaluationBatchStatus.COMPLETED_WITH_FAILURES
                : EvaluationBatchStatus.COMPLETED;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void markError(String failureReason) {
        LocalDateTime now = LocalDateTime.now();

        this.status = EvaluationBatchStatus.ERROR;
        this.failureReasons = List.of(failureReason);
        this.completedAt = now;
        this.updatedAt = now;
    }
}