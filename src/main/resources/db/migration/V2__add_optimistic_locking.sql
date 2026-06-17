-- Optimistic locking support: Hibernate @Version columns.
-- Existing rows start at version 0; the NOT NULL DEFAULT keeps backfill safe.
ALTER TABLE payments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE refunds  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
