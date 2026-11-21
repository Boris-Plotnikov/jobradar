ALTER TABLE ai_usage ADD COLUMN vacancy_id BIGINT;
ALTER TABLE ai_usage ADD COLUMN duration_ms INT;
CREATE INDEX ix_ai_usage_vacancy ON ai_usage (vacancy_id);
