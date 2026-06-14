package com.aliniaz.llmeval.evaluationcase.domain;

import com.aliniaz.llmeval.workflow.domain.AiWorkflow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(
        name = "evaluation_cases",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_evaluation_cases_workflow_name",
                        columnNames = {"workflow_id", "name"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private AiWorkflow workflow;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 80)
    private String taskType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> inputPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> expectedOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> requiredFacts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> forbiddenClaims;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> scoringRules;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public EvaluationCase(
            AiWorkflow workflow,
            String name,
            String taskType,
            Map<String, Object> inputPayload,
            Map<String, Object> expectedOutput,
            List<String> requiredFacts,
            List<String> forbiddenClaims,
            Map<String, Object> scoringRules
    ) {
        this.workflow = workflow;
        this.name = name;
        this.taskType = taskType;
        this.inputPayload = inputPayload;
        this.expectedOutput = expectedOutput;
        this.requiredFacts = requiredFacts;
        this.forbiddenClaims = forbiddenClaims;
        this.scoringRules = scoringRules;
        this.enabled = true;
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
}