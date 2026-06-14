package com.aliniaz.llmeval.evaluationrun.domain;

import com.aliniaz.llmeval.evaluationcase.domain.EvaluationCase;
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
@Table(name = "evaluation_runs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_version_id", nullable = false)
    private PromptVersion promptVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evaluation_case_id", nullable = false)
    private EvaluationCase evaluationCase;

    @Column(nullable = false, length = 120)
    private String modelName;

    @Column(length = 120)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private EvaluationRunProvider provider = EvaluationRunProvider.OLLAMA;

    @Column(columnDefinition = "TEXT")
    private String rawOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> parsedOutput;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(precision = 4, scale = 2)
    private BigDecimal temperature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> runConfig;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvaluationRunStatus status = EvaluationRunStatus.PENDING;

    private Boolean passed;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> failureReasons;

    @Column(columnDefinition = "TEXT")
    private String reviewerNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public EvaluationRun(
            AiWorkflow workflow,
            PromptVersion promptVersion,
            EvaluationCase evaluationCase,
            String modelName,
            String modelVersion,
            EvaluationRunProvider provider,
            BigDecimal temperature,
            Map<String, Object> runConfig
    ) {
        this.workflow = workflow;
        this.promptVersion = promptVersion;
        this.evaluationCase = evaluationCase;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.provider = provider;
        this.temperature = temperature;
        this.runConfig = runConfig;
        this.status = EvaluationRunStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.startedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void complete(
            String rawOutput,
            Map<String, Object> parsedOutput,
            BigDecimal confidence,
            Boolean passed,
            BigDecimal score,
            List<String> failureReasons,
            String reviewerNotes
    ) {
        this.rawOutput = rawOutput;
        this.parsedOutput = parsedOutput;
        this.confidence = confidence;
        this.passed = passed;
        this.score = score;
        this.failureReasons = failureReasons;
        this.reviewerNotes = reviewerNotes;
        this.status = Boolean.TRUE.equals(passed)
                ? EvaluationRunStatus.PASSED
                : EvaluationRunStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}