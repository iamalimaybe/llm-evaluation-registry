--liquibase formatted sql

--changeset aliniaz:007-add-batch-id-to-evaluation-runs
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'evaluation_runs' AND column_name = 'batch_id';
ALTER TABLE evaluation_runs
    ADD COLUMN batch_id BIGINT;

--rollback ALTER TABLE evaluation_runs DROP COLUMN IF EXISTS batch_id;

--changeset aliniaz:007-add-evaluation-runs-batch-foreign-key
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = 'public' AND table_name = 'evaluation_runs' AND constraint_name = 'fk_evaluation_runs_batch';
ALTER TABLE evaluation_runs
    ADD CONSTRAINT fk_evaluation_runs_batch
        FOREIGN KEY (batch_id)
            REFERENCES evaluation_batches (id)
            ON DELETE SET NULL;

--rollback ALTER TABLE evaluation_runs DROP CONSTRAINT IF EXISTS fk_evaluation_runs_batch;

--changeset aliniaz:007-create-evaluation-runs-batch-id-index
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_evaluation_runs_batch_id';
CREATE INDEX idx_evaluation_runs_batch_id
    ON evaluation_runs (batch_id);

--rollback DROP INDEX IF EXISTS idx_evaluation_runs_batch_id;