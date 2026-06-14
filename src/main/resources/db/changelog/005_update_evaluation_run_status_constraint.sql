--liquibase formatted sql

--changeset aliniaz:005-update-evaluation-run-status-constraint
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'evaluation_runs'
ALTER TABLE evaluation_runs
DROP CONSTRAINT IF EXISTS chk_evaluation_runs_status;

ALTER TABLE evaluation_runs
    ADD CONSTRAINT chk_evaluation_runs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'OUTPUT_CAPTURED', 'PASSED', 'FAILED', 'ERROR'));

--rollback ALTER TABLE evaluation_runs DROP CONSTRAINT IF EXISTS chk_evaluation_runs_status;
--rollback ALTER TABLE evaluation_runs ADD CONSTRAINT chk_evaluation_runs_status CHECK (status IN ('PENDING', 'RUNNING', 'PASSED', 'FAILED', 'ERROR'));