CREATE DATABASE IF NOT EXISTS basededatosupeckmarquetspringboot
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE basededatosupeckmarquetspringboot;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email VARCHAR(254) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  profile_photo_url VARCHAR(500) NULL,
  payout_bank VARCHAR(80) NULL,
  payout_account VARCHAR(120) NULL,
  payout_holder VARCHAR(120) NULL,
  payout_identifier VARCHAR(120) NULL,
  account_status ENUM('active', 'restricted', 'suspended') NOT NULL DEFAULT 'active',
  two_factor_secret VARCHAR(255) NULL,
  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  KEY idx_users_status (account_status)
) ENGINE=InnoDB;

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

CREATE TABLE IF NOT EXISTS roles (
  id SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name ENUM('BUYER', 'SELLER', 'ADMIN', 'SUPER_ADMIN') NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id SMALLINT UNSIGNED NOT NULL,
  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS categories (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  slug VARCHAR(90) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  UNIQUE KEY uq_categories_name (name),
  UNIQUE KEY uq_categories_slug (slug)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS products (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  seller_id BIGINT UNSIGNED NOT NULL,
  category_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(160) NOT NULL,
  description TEXT NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  condition_type ENUM('new', 'like_new', 'good', 'fair') NOT NULL,
  listing_type ENUM('sale', 'exchange', 'both') NOT NULL DEFAULT 'sale',
  status ENUM('pending', 'approved', 'rejected', 'reserved', 'sold', 'exchanged', 'deleted') NOT NULL DEFAULT 'pending',
  image_url VARCHAR(1000) NULL,
  reserved_by BIGINT UNSIGNED NULL,
  reservation_expires_at DATETIME NULL,
  reviewed_by BIGINT UNSIGNED NULL,
  reviewed_at DATETIME NULL,
  review_note VARCHAR(500) NULL,
  deleted_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_products_catalog (status, category_id, created_at),
  KEY idx_products_seller (seller_id, deleted_at),
  KEY idx_products_expired_reservations (status, reservation_expires_at),
  CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users (id),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
  CONSTRAINT fk_products_reserved_by FOREIGN KEY (reserved_by) REFERENCES users (id),
  CONSTRAINT fk_products_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
  CONSTRAINT chk_products_price CHECK (price >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS favorites (
  user_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, product_id),
  CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_favorites_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NOT NULL,
  product_id BIGINT UNSIGNED NOT NULL,
  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_cart_user_product (user_id, product_id),
  KEY idx_cart_product (product_id),
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS transactions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  product_id BIGINT UNSIGNED NOT NULL,
  buyer_id BIGINT UNSIGNED NOT NULL,
  seller_id BIGINT UNSIGNED NOT NULL,
  transaction_type ENUM('purchase', 'exchange') NOT NULL,
  status ENUM('pending_payment', 'paid', 'in_transit', 'delivered', 'exchanged', 'cancelled') NOT NULL DEFAULT 'pending_payment',
  amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  payment_method VARCHAR(50) NULL,
  payment_reference VARCHAR(120) NULL,
  payment_proof_url VARCHAR(1000) NULL,
  platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  seller_payout DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  payout_status VARCHAR(30) NOT NULL DEFAULT 'not_applicable',
  exchange_details TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_transactions_buyer (buyer_id, created_at),
  KEY idx_transactions_seller (seller_id, created_at),
  KEY idx_transactions_product (product_id, status),
  CONSTRAINT fk_transactions_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_transactions_buyer FOREIGN KEY (buyer_id) REFERENCES users (id),
  CONSTRAINT fk_transactions_seller FOREIGN KEY (seller_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  transaction_id BIGINT UNSIGNED NULL,
  sender_id BIGINT UNSIGNED NOT NULL,
  recipient_id BIGINT UNSIGNED NOT NULL,
  body TEXT NOT NULL,
  read_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_messages_conversation (sender_id, recipient_id, created_at),
  KEY idx_messages_transaction (transaction_id, created_at),
  CONSTRAINT fk_messages_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id),
  CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reviews (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  transaction_id BIGINT UNSIGNED NOT NULL,
  reviewer_id BIGINT UNSIGNED NOT NULL,
  reviewee_id BIGINT UNSIGNED NOT NULL,
  rating TINYINT UNSIGNED NOT NULL,
  comment VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_reviews_transaction_reviewer (transaction_id, reviewer_id),
  KEY idx_reviews_reviewee (reviewee_id, created_at),
  CONSTRAINT fk_reviews_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
  CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id),
  CONSTRAINT fk_reviews_reviewee FOREIGN KEY (reviewee_id) REFERENCES users (id),
  CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reports (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reporter_id BIGINT UNSIGNED NOT NULL,
  reported_user_id BIGINT UNSIGNED NULL,
  product_id BIGINT UNSIGNED NULL,
  reason VARCHAR(120) NOT NULL,
  details TEXT NULL,
  status ENUM('open', 'investigating', 'resolved', 'dismissed') NOT NULL DEFAULT 'open',
  resolved_by BIGINT UNSIGNED NULL,
  resolved_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_reports_inbox (status, created_at),
  CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
  CONSTRAINT fk_reports_reported_user FOREIGN KEY (reported_user_id) REFERENCES users (id),
  CONSTRAINT fk_reports_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_reports_resolver FOREIGN KEY (resolved_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS support_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  email VARCHAR(254) NOT NULL,
  subject VARCHAR(160) NOT NULL,
  message TEXT NOT NULL,
  status ENUM('open', 'in_progress', 'closed') NOT NULL DEFAULT 'open',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_support_status (status, created_at),
  CONSTRAINT fk_support_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS system_settings (
  setting_key VARCHAR(100) NOT NULL,
  setting_value TEXT NOT NULL,
  updated_by BIGINT UNSIGNED NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (setting_key),
  CONSTRAINT fk_settings_admin FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS security_audit_log (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  actor_id BIGINT UNSIGNED NULL,
  action VARCHAR(100) NOT NULL,
  target_type VARCHAR(60) NULL,
  target_id BIGINT UNSIGNED NULL,
  details JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_actor (actor_id, created_at),
  KEY idx_audit_action (action, created_at),
  CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users (id)
) ENGINE=InnoDB;

INSERT IGNORE INTO roles (name) VALUES
  ('BUYER'), ('SELLER'), ('ADMIN'), ('SUPER_ADMIN');

INSERT IGNORE INTO categories (name, slug) VALUES
  ('Tecnología', 'tecnologia'),
  ('Libros', 'libros'),
  ('Ropa', 'ropa'),
  ('Hogar', 'hogar'),
  ('Deportes', 'deportes'),
  ('Otros', 'otros');
