CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  profile_photo_url VARCHAR(500),
  payout_bank VARCHAR(80),
  payout_account VARCHAR(120),
  payout_holder VARCHAR(120),
  payout_identifier VARCHAR(120),
  account_status VARCHAR(20) NOT NULL DEFAULT 'active',
  two_factor_secret VARCHAR(255),
  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_sessions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auth_sessions_expiration ON auth_sessions(expires_at);

CREATE TABLE roles (
  id SMALLSERIAL PRIMARY KEY,
  name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL REFERENCES users(id),
  role_id SMALLINT NOT NULL REFERENCES roles(id),
  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE categories (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  slug VARCHAR(90) NOT NULL UNIQUE,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE products (
  id BIGSERIAL PRIMARY KEY,
  seller_id BIGINT NOT NULL REFERENCES users(id),
  category_id BIGINT NOT NULL REFERENCES categories(id),
  title VARCHAR(160) NOT NULL,
  description TEXT NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  condition_type VARCHAR(20) NOT NULL,
  listing_type VARCHAR(20) NOT NULL DEFAULT 'sale',
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  image_url VARCHAR(1000),
  reserved_by BIGINT REFERENCES users(id),
  reservation_expires_at TIMESTAMP,
  reviewed_by BIGINT REFERENCES users(id),
  reviewed_at TIMESTAMP,
  review_note VARCHAR(500),
  deleted_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_products_price CHECK (price >= 0)
);
CREATE INDEX idx_products_catalog ON products(status, category_id, created_at);
CREATE INDEX idx_products_seller ON products(seller_id, deleted_at);
CREATE INDEX idx_products_expired_reservations ON products(status, reservation_expires_at);

CREATE TABLE favorites (
  user_id BIGINT NOT NULL REFERENCES users(id),
  product_id BIGINT NOT NULL REFERENCES products(id),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, product_id)
);

CREATE TABLE cart_items (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  product_id BIGINT NOT NULL REFERENCES products(id),
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, product_id)
);
CREATE INDEX idx_cart_product ON cart_items(product_id);

CREATE TABLE transactions (
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES products(id),
  buyer_id BIGINT NOT NULL REFERENCES users(id),
  seller_id BIGINT NOT NULL REFERENCES users(id),
  transaction_type VARCHAR(20) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'pending_payment',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  payment_method VARCHAR(50),
  payment_reference VARCHAR(120),
  payment_proof_url VARCHAR(1000),
  platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  seller_payout DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  payout_status VARCHAR(30) NOT NULL DEFAULT 'not_applicable',
  exchange_details TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE messages (
  id BIGSERIAL PRIMARY KEY,
  transaction_id BIGINT REFERENCES transactions(id),
  sender_id BIGINT NOT NULL REFERENCES users(id),
  recipient_id BIGINT NOT NULL REFERENCES users(id),
  body TEXT NOT NULL,
  read_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_messages_conversation ON messages(sender_id, recipient_id, created_at);

CREATE TABLE reviews (
  id BIGSERIAL PRIMARY KEY,
  transaction_id BIGINT NOT NULL REFERENCES transactions(id),
  reviewer_id BIGINT NOT NULL REFERENCES users(id),
  reviewee_id BIGINT NOT NULL REFERENCES users(id),
  rating SMALLINT NOT NULL,
  comment VARCHAR(1000),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (transaction_id, reviewer_id),
  CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE reports (
  id BIGSERIAL PRIMARY KEY,
  reporter_id BIGINT NOT NULL REFERENCES users(id),
  reported_user_id BIGINT REFERENCES users(id),
  product_id BIGINT REFERENCES products(id),
  reason VARCHAR(120) NOT NULL,
  details TEXT,
  status VARCHAR(30) NOT NULL DEFAULT 'open',
  resolved_by BIGINT REFERENCES users(id),
  resolved_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE support_requests (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  email VARCHAR(254) NOT NULL,
  subject VARCHAR(160) NOT NULL,
  message TEXT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'open',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE system_settings (
  setting_key VARCHAR(100) PRIMARY KEY,
  setting_value TEXT NOT NULL,
  updated_by BIGINT NOT NULL REFERENCES users(id),
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE security_audit_log (
  id BIGSERIAL PRIMARY KEY,
  actor_id BIGINT REFERENCES users(id),
  action VARCHAR(100) NOT NULL,
  target_type VARCHAR(60),
  target_id BIGINT,
  details JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO roles (name) VALUES ('BUYER'), ('SELLER'), ('ADMIN'), ('SUPER_ADMIN');
INSERT INTO categories (name, slug) VALUES
  ('Tecnología', 'tecnologia'),
  ('Libros', 'libros'),
  ('Ropa', 'ropa'),
  ('Hogar', 'hogar'),
  ('Deportes', 'deportes'),
  ('Otros', 'otros');
