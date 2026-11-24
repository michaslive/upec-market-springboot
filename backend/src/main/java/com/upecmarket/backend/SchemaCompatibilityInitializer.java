package com.upecmarket.backend;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class SchemaCompatibilityInitializer implements ApplicationRunner {
	private final JdbcTemplate jdbc;

	public SchemaCompatibilityInitializer(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void run(ApplicationArguments args) {
		String productName;
		try (java.sql.Connection connection = jdbc.getDataSource().getConnection()) {
			productName = connection.getMetaData().getDatabaseProductName().toLowerCase();
		} catch (java.sql.SQLException exception) {
			throw new IllegalStateException("No se pudo inspeccionar la base de datos.", exception);
		}
		if (!productName.contains("postgresql")) {
			return;
		}

		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS users (
				  id BIGSERIAL PRIMARY KEY,
				  email VARCHAR(254) UNIQUE,
				  password_hash VARCHAR(255),
				  password VARCHAR(255),
				  name VARCHAR(120),
				  full_name VARCHAR(120),
				  profile_photo_url VARCHAR(500),
				  payout_bank VARCHAR(80),
				  payout_account VARCHAR(120),
				  payout_holder VARCHAR(120),
				  payout_identifier VARCHAR(120),
				  account_status VARCHAR(20) NOT NULL DEFAULT 'active',
				  two_factor_secret VARCHAR(255),
				  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
				  remember_token VARCHAR(100),
				  email_verified_at TIMESTAMP,
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(254)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(120)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(120)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(500)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_bank VARCHAR(80)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_account VARCHAR(120)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_holder VARCHAR(120)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_identifier VARCHAR(120)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'active'");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS remember_token VARCHAR(100)");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
		jdbc.execute("UPDATE users SET account_status = 'active' WHERE account_status IS NULL");
		jdbc.execute("UPDATE users SET full_name = COALESCE(full_name, name, split_part(COALESCE(email, 'Usuario'), '@', 1))");

		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS auth_sessions (
				  id BIGSERIAL PRIMARY KEY,
				  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
				  token_hash VARCHAR(64) NOT NULL UNIQUE,
				  expires_at TIMESTAMP NOT NULL,
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbc.execute("CREATE INDEX IF NOT EXISTS idx_auth_sessions_expiration ON auth_sessions(expires_at)");

		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS roles (
				  id SMALLSERIAL PRIMARY KEY,
				  name VARCHAR(20) NOT NULL UNIQUE
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS user_roles (
				  user_id BIGINT NOT NULL REFERENCES users(id),
				  role_id SMALLINT NOT NULL REFERENCES roles(id),
				  assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (user_id, role_id)
				)
				""");
		for (String role : new String[] {"BUYER", "SELLER", "ADMIN", "SUPER_ADMIN"}) {
			jdbc.update("INSERT INTO roles (name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = ?)", role, role);
		}

		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS categories (
				  id BIGSERIAL PRIMARY KEY,
				  name VARCHAR(80) NOT NULL UNIQUE,
				  slug VARCHAR(90) NOT NULL UNIQUE,
				  active BOOLEAN NOT NULL DEFAULT TRUE
				)
				""");
		jdbc.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS slug VARCHAR(90)");
		jdbc.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE");
		jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Tecnología', 'tecnologia', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'tecnologia')");
		jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Libros', 'libros', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'libros')");
		jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Ropa', 'ropa', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'ropa')");
		jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Hogar', 'hogar', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'hogar')");
		jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Deportes', 'deportes', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'deportes')");
		jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Otros', 'otros', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE slug = 'otros')");

		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS products (
				  id BIGSERIAL PRIMARY KEY,
				  seller_id BIGINT NOT NULL,
				  category_id BIGINT NOT NULL,
				  title VARCHAR(160) NOT NULL,
				  description TEXT NOT NULL,
				  price DECIMAL(10,2) NOT NULL DEFAULT 0,
				  condition_type VARCHAR(20) NOT NULL DEFAULT 'good',
				  listing_type VARCHAR(20) NOT NULL DEFAULT 'sale',
				  status VARCHAR(20) NOT NULL DEFAULT 'approved',
				  image_url VARCHAR(1000),
				  reserved_by BIGINT,
				  reservation_expires_at TIMESTAMP,
				  reviewed_by BIGINT,
				  reviewed_at TIMESTAMP,
				  review_note VARCHAR(500),
				  deleted_at TIMESTAMP,
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS seller_id BIGINT");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS category_id BIGINT");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS title VARCHAR(160)");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS description TEXT");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) NOT NULL DEFAULT 0");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS condition_type VARCHAR(20) NOT NULL DEFAULT 'good'");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS listing_type VARCHAR(20) NOT NULL DEFAULT 'sale'");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'approved'");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000)");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reserved_by BIGINT");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reservation_expires_at TIMESTAMP");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reviewed_by BIGINT");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS review_note VARCHAR(500)");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
		jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
		jdbc.execute("UPDATE products SET status = 'approved' WHERE status IS NULL");
		jdbc.execute("CREATE INDEX IF NOT EXISTS idx_products_catalog ON products(status, category_id, created_at)");

		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS favorites (
				  user_id BIGINT NOT NULL REFERENCES users(id),
				  product_id BIGINT NOT NULL REFERENCES products(id),
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (user_id, product_id)
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS cart_items (
				  id BIGSERIAL PRIMARY KEY,
				  user_id BIGINT NOT NULL REFERENCES users(id),
				  product_id BIGINT NOT NULL REFERENCES products(id),
				  added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  UNIQUE (user_id, product_id)
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS transactions (
				  id BIGSERIAL PRIMARY KEY,
				  product_id BIGINT NOT NULL REFERENCES products(id),
				  buyer_id BIGINT NOT NULL REFERENCES users(id),
				  seller_id BIGINT NOT NULL REFERENCES users(id),
				  transaction_type VARCHAR(20) NOT NULL,
				  status VARCHAR(30) NOT NULL DEFAULT 'pending_payment',
				  amount DECIMAL(10,2) NOT NULL DEFAULT 0,
				  payment_method VARCHAR(50),
				  payment_reference VARCHAR(120),
				  payment_proof_url VARCHAR(1000),
				  platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
				  seller_payout DECIMAL(10,2) NOT NULL DEFAULT 0,
				  payout_status VARCHAR(30) NOT NULL DEFAULT 'not_applicable',
				  exchange_details TEXT,
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS messages (
				  id BIGSERIAL PRIMARY KEY,
				  transaction_id BIGINT REFERENCES transactions(id),
				  sender_id BIGINT NOT NULL REFERENCES users(id),
				  recipient_id BIGINT NOT NULL REFERENCES users(id),
				  body TEXT NOT NULL,
				  read_at TIMESTAMP,
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS reviews (
				  id BIGSERIAL PRIMARY KEY,
				  transaction_id BIGINT NOT NULL REFERENCES transactions(id),
				  reviewer_id BIGINT NOT NULL REFERENCES users(id),
				  reviewee_id BIGINT NOT NULL REFERENCES users(id),
				  rating SMALLINT NOT NULL,
				  comment VARCHAR(1000),
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  UNIQUE (transaction_id, reviewer_id)
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS reports (
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
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS support_requests (
				  id BIGSERIAL PRIMARY KEY,
				  user_id BIGINT REFERENCES users(id),
				  email VARCHAR(254) NOT NULL,
				  subject VARCHAR(160) NOT NULL,
				  message TEXT NOT NULL,
				  status VARCHAR(30) NOT NULL DEFAULT 'open',
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS system_settings (
				  setting_key VARCHAR(100) PRIMARY KEY,
				  setting_value TEXT NOT NULL,
				  updated_by BIGINT,
				  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS security_audit_log (
				  id BIGSERIAL PRIMARY KEY,
				  actor_id BIGINT,
				  action VARCHAR(100) NOT NULL,
				  target_type VARCHAR(60),
				  target_id BIGINT,
				  details JSONB,
				  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
	}
}
