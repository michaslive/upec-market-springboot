USE basededatosupeckmarquetspringboot;

CREATE TABLE IF NOT EXISTS auth_sessions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_auth_sessions_token_hash (token_hash),
  KEY idx_auth_sessions_expiration (expires_at),
  CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE products
  ADD COLUMN IF NOT EXISTS reserved_by BIGINT UNSIGNED NULL AFTER image_url;

ALTER TABLE transactions
  ADD COLUMN IF NOT EXISTS payment_proof_url VARCHAR(1000) NULL AFTER payment_reference;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS payout_bank VARCHAR(80) NULL,
  ADD COLUMN IF NOT EXISTS payout_account VARCHAR(120) NULL,
  ADD COLUMN IF NOT EXISTS payout_holder VARCHAR(120) NULL,
  ADD COLUMN IF NOT EXISTS payout_identifier VARCHAR(120) NULL;

ALTER TABLE transactions
  ADD COLUMN IF NOT EXISTS platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN IF NOT EXISTS seller_payout DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  ADD COLUMN IF NOT EXISTS payout_status VARCHAR(30) NOT NULL DEFAULT 'not_applicable';
