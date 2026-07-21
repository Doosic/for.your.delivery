CREATE TABLE IF NOT EXISTS tb_fy_product (
  product_sq BIGSERIAL PRIMARY KEY,
  product_key VARCHAR(255) NOT NULL,
  name VARCHAR(500) NOT NULL,
  normalized_name VARCHAR(500) NOT NULL,
  brand VARCHAR(200),
  maker VARCHAR(200),
  category_path VARCHAR(500),
  image_url TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_product_key UNIQUE (product_key)
);

CREATE INDEX IF NOT EXISTS idx_product_normalized_name
  ON tb_fy_product(normalized_name);
CREATE INDEX IF NOT EXISTS idx_product_status
  ON tb_fy_product(status, modified_date DESC);

CREATE TABLE IF NOT EXISTS tb_fy_product_offer (
  offer_sq BIGSERIAL PRIMARY KEY,
  product_sq BIGINT NOT NULL,
  provider VARCHAR(30) NOT NULL,
  external_product_id VARCHAR(255) NOT NULL,
  mall_name VARCHAR(200),
  product_url TEXT NOT NULL,
  price BIGINT NOT NULL,
  shipping_fee BIGINT NOT NULL DEFAULT 0,
  total_price BIGINT NOT NULL,
  search_rank INTEGER,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_collected_at TIMESTAMP NOT NULL,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_offer_product FOREIGN KEY (product_sq)
    REFERENCES tb_fy_product(product_sq),
  CONSTRAINT uq_product_offer_provider_external UNIQUE (provider, external_product_id)
);

CREATE INDEX IF NOT EXISTS idx_product_offer_product_price
  ON tb_fy_product_offer(product_sq, total_price);
CREATE INDEX IF NOT EXISTS idx_product_offer_provider_rank
  ON tb_fy_product_offer(provider, search_rank, last_collected_at DESC);

CREATE TABLE IF NOT EXISTS tb_fy_product_price_history (
  price_history_sq BIGSERIAL PRIMARY KEY,
  offer_sq BIGINT NOT NULL,
  price BIGINT NOT NULL,
  shipping_fee BIGINT NOT NULL DEFAULT 0,
  total_price BIGINT NOT NULL,
  collected_at TIMESTAMP NOT NULL,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_price_history_offer FOREIGN KEY (offer_sq)
    REFERENCES tb_fy_product_offer(offer_sq) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_price_history_offer_collected
  ON tb_fy_product_price_history(offer_sq, collected_at DESC);
CREATE INDEX IF NOT EXISTS idx_price_history_collected
  ON tb_fy_product_price_history(collected_at DESC);

CREATE TABLE IF NOT EXISTS tb_fy_purchase_click (
  purchase_click_sq BIGSERIAL PRIMARY KEY,
  user_sq BIGINT,
  product_sq BIGINT NOT NULL,
  offer_sq BIGINT NOT NULL,
  provider VARCHAR(30) NOT NULL,
  external_product_id VARCHAR(255) NOT NULL,
  target_url TEXT NOT NULL,
  price_at_click BIGINT NOT NULL,
  source_context VARCHAR(30) NOT NULL DEFAULT 'OTHER',
  clicked_at TIMESTAMP NOT NULL,
  create_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_purchase_click_user FOREIGN KEY (user_sq)
    REFERENCES tb_fy_user(user_sq),
  CONSTRAINT fk_purchase_click_product FOREIGN KEY (product_sq)
    REFERENCES tb_fy_product(product_sq),
  CONSTRAINT fk_purchase_click_offer FOREIGN KEY (offer_sq)
    REFERENCES tb_fy_product_offer(offer_sq)
);

CREATE INDEX IF NOT EXISTS idx_purchase_click_offer_clicked
  ON tb_fy_purchase_click(offer_sq, clicked_at DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_click_user_clicked
  ON tb_fy_purchase_click(user_sq, clicked_at DESC);
