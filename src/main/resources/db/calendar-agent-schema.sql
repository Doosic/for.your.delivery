CREATE TABLE IF NOT EXISTS tb_fy_calendar_event (
  calendar_event_sq BIGSERIAL PRIMARY KEY,
  user_sq BIGINT NOT NULL,
  provider VARCHAR(30) NOT NULL,
  provider_calendar_id VARCHAR(255) NOT NULL,
  external_event_id VARCHAR(255) NOT NULL,
  title VARCHAR(500) NOT NULL,
  description TEXT,
  location VARCHAR(500),
  starts_at TIMESTAMP NOT NULL,
  ends_at TIMESTAMP NOT NULL,
  all_day BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_synced_at TIMESTAMP NOT NULL,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_calendar_event_user FOREIGN KEY (user_sq)
    REFERENCES tb_fy_user(user_sq),
  CONSTRAINT uq_calendar_event_external
    UNIQUE (user_sq, provider, provider_calendar_id, external_event_id)
);

CREATE INDEX IF NOT EXISTS idx_calendar_event_user_start
  ON tb_fy_calendar_event(user_sq, starts_at, status);

CREATE TABLE IF NOT EXISTS tb_fy_calendar_item_suggestion (
  calendar_suggestion_sq BIGSERIAL PRIMARY KEY,
  calendar_event_sq BIGINT NOT NULL,
  user_sq BIGINT NOT NULL,
  keyword VARCHAR(200) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  priority VARCHAR(20) NOT NULL,
  recommended_buy_by DATE NOT NULL,
  product_sq BIGINT,
  offer_sq BIGINT,
  product_name VARCHAR(500),
  provider VARCHAR(30),
  provider_code VARCHAR(255),
  product_url TEXT,
  image_url TEXT,
  price BIGINT,
  product_live BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  generated_at TIMESTAMP NOT NULL,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_calendar_suggestion_event FOREIGN KEY (calendar_event_sq)
    REFERENCES tb_fy_calendar_event(calendar_event_sq) ON DELETE CASCADE,
  CONSTRAINT fk_calendar_suggestion_user FOREIGN KEY (user_sq)
    REFERENCES tb_fy_user(user_sq),
  CONSTRAINT fk_calendar_suggestion_product FOREIGN KEY (product_sq)
    REFERENCES tb_fy_product(product_sq),
  CONSTRAINT fk_calendar_suggestion_offer FOREIGN KEY (offer_sq)
    REFERENCES tb_fy_product_offer(offer_sq)
);

CREATE INDEX IF NOT EXISTS idx_calendar_suggestion_event
  ON tb_fy_calendar_item_suggestion(calendar_event_sq, status);
CREATE INDEX IF NOT EXISTS idx_calendar_suggestion_user_buy_by
  ON tb_fy_calendar_item_suggestion(user_sq, recommended_buy_by, status);
