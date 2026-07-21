CREATE TABLE IF NOT EXISTS tb_fy_agent_run (
  agent_run_sq BIGSERIAL PRIMARY KEY,
  user_sq BIGINT NOT NULL,
  agent_session_sq BIGINT NOT NULL,
  trigger_message_sq BIGINT,
  agent_type VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
  input_summary_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  output_summary_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  model_name VARCHAR(100),
  prompt_version VARCHAR(30) NOT NULL,
  started_at TIMESTAMP NOT NULL,
  completed_at TIMESTAMP,
  error_code VARCHAR(50),
  error_message VARCHAR(500),
  trace_id VARCHAR(100) NOT NULL,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_run_user FOREIGN KEY (user_sq) REFERENCES tb_fy_user(user_sq),
  CONSTRAINT fk_agent_run_session FOREIGN KEY (agent_session_sq)
    REFERENCES tb_fy_agent_session(agent_session_sq) ON DELETE CASCADE,
  CONSTRAINT fk_agent_run_trigger_message FOREIGN KEY (trigger_message_sq)
    REFERENCES tb_fy_agent_message(agent_message_sq) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_agent_run_session_created
  ON tb_fy_agent_run(agent_session_sq, create_date DESC);
CREATE INDEX IF NOT EXISTS idx_agent_run_user_agent_created
  ON tb_fy_agent_run(user_sq, agent_type, create_date DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_agent_run_trace_id
  ON tb_fy_agent_run(trace_id);

ALTER TABLE tb_fy_agent_message
  ADD COLUMN IF NOT EXISTS agent_run_sq BIGINT;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_agent_message_run') THEN
    ALTER TABLE tb_fy_agent_message
      ADD CONSTRAINT fk_agent_message_run
      FOREIGN KEY (agent_run_sq) REFERENCES tb_fy_agent_run(agent_run_sq) ON DELETE SET NULL;
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_agent_message_run
  ON tb_fy_agent_message(agent_run_sq);

CREATE TABLE IF NOT EXISTS tb_fy_agent_decision (
  agent_decision_sq BIGSERIAL PRIMARY KEY,
  agent_run_sq BIGINT NOT NULL,
  user_sq BIGINT NOT NULL,
  decision_type VARCHAR(30) NOT NULL,
  decision VARCHAR(50) NOT NULL,
  score NUMERIC(6,5),
  target_type VARCHAR(30),
  target_sq BIGINT,
  reason_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  evidence_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  expires_at TIMESTAMP,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_decision_run FOREIGN KEY (agent_run_sq)
    REFERENCES tb_fy_agent_run(agent_run_sq) ON DELETE CASCADE,
  CONSTRAINT fk_agent_decision_user FOREIGN KEY (user_sq) REFERENCES tb_fy_user(user_sq)
);

CREATE INDEX IF NOT EXISTS idx_agent_decision_run
  ON tb_fy_agent_decision(agent_run_sq, agent_decision_sq);
CREATE INDEX IF NOT EXISTS idx_agent_decision_user_status
  ON tb_fy_agent_decision(user_sq, decision, status, create_date DESC);

CREATE TABLE IF NOT EXISTS tb_fy_wiki_entry (
  wiki_entry_sq BIGSERIAL PRIMARY KEY,
  user_sq BIGINT NOT NULL,
  category VARCHAR(30) NOT NULL,
  entry_key VARCHAR(100) NOT NULL,
  summary VARCHAR(500) NOT NULL,
  content_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  source_type VARCHAR(30) NOT NULL,
  source_ref_sq BIGINT,
  confidence NUMERIC(5,4) NOT NULL,
  status VARCHAR(30) NOT NULL,
  sensitivity_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  version INTEGER NOT NULL DEFAULT 1,
  valid_until DATE,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wiki_entry_user FOREIGN KEY (user_sq) REFERENCES tb_fy_user(user_sq),
  CONSTRAINT uq_wiki_entry_user_key UNIQUE (user_sq, category, entry_key)
);

CREATE INDEX IF NOT EXISTS idx_wiki_entry_user_status
  ON tb_fy_wiki_entry(user_sq, status, modified_date DESC);

CREATE TABLE IF NOT EXISTS tb_fy_wiki_entry_history (
  wiki_history_sq BIGSERIAL PRIMARY KEY,
  wiki_entry_sq BIGINT NOT NULL,
  user_sq BIGINT NOT NULL,
  version INTEGER NOT NULL,
  snapshot_json JSONB NOT NULL,
  changed_by VARCHAR(30) NOT NULL,
  change_reason VARCHAR(200),
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wiki_history_entry FOREIGN KEY (wiki_entry_sq)
    REFERENCES tb_fy_wiki_entry(wiki_entry_sq) ON DELETE CASCADE,
  CONSTRAINT fk_wiki_history_user FOREIGN KEY (user_sq) REFERENCES tb_fy_user(user_sq),
  CONSTRAINT uq_wiki_history_version UNIQUE (wiki_entry_sq, version)
);

CREATE INDEX IF NOT EXISTS idx_wiki_history_entry_version
  ON tb_fy_wiki_entry_history(wiki_entry_sq, version DESC);
