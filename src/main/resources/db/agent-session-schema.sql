CREATE TABLE IF NOT EXISTS tb_fy_agent_session (
  agent_session_sq BIGSERIAL PRIMARY KEY,
  user_sq BIGINT NOT NULL,
  session_type VARCHAR(30) NOT NULL DEFAULT 'UNIFIED',
  title VARCHAR(200),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  context_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  last_message_at TIMESTAMP,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_session_user
    FOREIGN KEY (user_sq) REFERENCES tb_fy_user(user_sq)
);

CREATE INDEX IF NOT EXISTS idx_agent_session_user_status
  ON tb_fy_agent_session(user_sq, status, last_message_at DESC);

CREATE TABLE IF NOT EXISTS tb_fy_agent_message (
  agent_message_sq BIGSERIAL PRIMARY KEY,
  agent_session_sq BIGINT NOT NULL,
  user_sq BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
  content TEXT,
  payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
  actions_json JSONB NOT NULL DEFAULT '[]'::jsonb,
  source_agent VARCHAR(50),
  trace_id VARCHAR(100),
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_agent_message_session
    FOREIGN KEY (agent_session_sq) REFERENCES tb_fy_agent_session(agent_session_sq) ON DELETE CASCADE,
  CONSTRAINT fk_agent_message_user
    FOREIGN KEY (user_sq) REFERENCES tb_fy_user(user_sq)
);

CREATE INDEX IF NOT EXISTS idx_agent_message_session_order
  ON tb_fy_agent_message(agent_session_sq, agent_message_sq);

CREATE INDEX IF NOT EXISTS idx_agent_message_user_created
  ON tb_fy_agent_message(user_sq, create_date DESC);
