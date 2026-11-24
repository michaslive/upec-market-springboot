package com.upecmarket.backend;


import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaCompatibilityInitializer implements ApplicationRunner {
	private final JdbcTemplate jdbc;

	public SchemaCompatibilityInitializer(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String databaseProductName = "";
		if (jdbc.getDataSource() != null) {
			try (java.sql.Connection connection = jdbc.getDataSource().getConnection()) {
				databaseProductName = connection.getMetaData().getDatabaseProductName();
			}
		}
		String normalized = databaseProductName.toLowerCase();
		if (normalized.contains("postgresql")) {
			Boolean usersTableExists = jdbc.queryForObject(
					"SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'users')",
					Boolean.class);
			if (!Boolean.TRUE.equals(usersTableExists)) {
				org.springframework.core.io.ClassPathResource resource =
						new org.springframework.core.io.ClassPathResource("db/migration/V1__create_marketplace_schema.sql");
				String sql = resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
				for (String statement : sql.split(";")) {
					String trimmed = statement.trim();
					if (!trimmed.isEmpty()) {
						jdbc.execute(trimmed);
					}
				}
			}
			Boolean usersExist = jdbc.queryForObject(
					"SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'users')",
					Boolean.class);
			if (Boolean.TRUE.equals(usersExist)) {
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(254)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(120)");
				jdbc.execute("""
						DO $$
						BEGIN
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'users'::regclass AND attname = 'email' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(254) USING convert_from(email, ''UTF8'')';
							END IF;
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'users'::regclass AND attname = 'password_hash' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE users ALTER COLUMN password_hash TYPE VARCHAR(255) USING convert_from(password_hash, ''UTF8'')';
							END IF;
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'users'::regclass AND attname = 'full_name' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE users ALTER COLUMN full_name TYPE VARCHAR(120) USING convert_from(full_name, ''UTF8'')';
							END IF;
						END $$;
						""");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_bank VARCHAR(80)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_account VARCHAR(120)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_holder VARCHAR(120)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_identifier VARCHAR(120)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(500)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'active'");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_secret VARCHAR(255)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
			}
			Boolean categoriesExist = jdbc.queryForObject(
					"SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'categories')",
					Boolean.class);
			if (Boolean.TRUE.equals(categoriesExist)) {
				jdbc.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS slug VARCHAR(90)");
				jdbc.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE");
				jdbc.execute("""
						DO $$
						BEGIN
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'categories'::regclass AND attname = 'name' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE categories ALTER COLUMN name TYPE VARCHAR(80) USING convert_from(name, ''UTF8'')';
							END IF;
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'categories'::regclass AND attname = 'slug' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE categories ALTER COLUMN slug TYPE VARCHAR(90) USING convert_from(slug, ''UTF8'')';
							END IF;
						END $$;
						""");
				jdbc.execute("UPDATE categories SET slug = LOWER(REPLACE(REPLACE(REPLACE(name, ' ', '-'), 'á', 'a'), 'é', 'e')) WHERE slug IS NULL");
				jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_slug_compat ON categories(slug)");
				jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Tecnología', 'tecnologia', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Tecnología')");
				jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Libros', 'libros', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Libros')");
				jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Ropa', 'ropa', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Ropa')");
				jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Hogar', 'hogar', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Hogar')");
				jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Deportes', 'deportes', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Deportes')");
				jdbc.update("INSERT INTO categories (name, slug, active) SELECT 'Otros', 'otros', TRUE WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Otros')");
			}
			Boolean productsExist = jdbc.queryForObject(
					"SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'products')",
					Boolean.class);
			if (Boolean.TRUE.equals(productsExist)) {
				jdbc.execute("""
						DO $$
						BEGIN
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'products'::regclass AND attname = 'title' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE products ALTER COLUMN title TYPE VARCHAR(160) USING convert_from(title, ''UTF8'')';
							END IF;
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'products'::regclass AND attname = 'description' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE products ALTER COLUMN description TYPE TEXT USING convert_from(description, ''UTF8'')';
							END IF;
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'products'::regclass AND attname = 'status' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE products ALTER COLUMN status TYPE VARCHAR(20) USING convert_from(status, ''UTF8'')';
							END IF;
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'products'::regclass AND attname = 'condition_type' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE products ALTER COLUMN condition_type TYPE VARCHAR(20) USING convert_from(condition_type, ''UTF8'')';
							END IF;
							IF EXISTS (SELECT 1 FROM pg_attribute WHERE attrelid = 'products'::regclass AND attname = 'listing_type' AND atttypid = 'bytea'::regtype) THEN
								EXECUTE 'ALTER TABLE products ALTER COLUMN listing_type TYPE VARCHAR(20) USING convert_from(listing_type, ''UTF8'')';
							END IF;
						END $$;
						""");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS condition_type VARCHAR(20) NOT NULL DEFAULT 'good'");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS listing_type VARCHAR(20) NOT NULL DEFAULT 'sale'");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'approved'");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reserved_by BIGINT");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reservation_expires_at TIMESTAMP");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reviewed_by BIGINT");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS review_note VARCHAR(500)");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
				jdbc.execute("ALTER TABLE products ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000)");
			}
			Boolean transactionsExist = jdbc.queryForObject(
					"SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'transactions')",
					Boolean.class);
			if (Boolean.TRUE.equals(transactionsExist)) {
				jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_proof_url VARCHAR(1000)");
				jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0");
				jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS seller_payout DECIMAL(10,2) NOT NULL DEFAULT 0");
				jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payout_status VARCHAR(30) NOT NULL DEFAULT 'not_applicable'");
			}
			return;
		}
		if (!normalized.contains("mariadb") && !normalized.contains("mysql")) {
			return;
		}
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_bank VARCHAR(80) NULL");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_account VARCHAR(120) NULL");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_holder VARCHAR(120) NULL");
		jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_identifier VARCHAR(120) NULL");
		jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_proof_url VARCHAR(1000) NULL");
		jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS platform_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00");
		jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS seller_payout DECIMAL(10,2) NOT NULL DEFAULT 0.00");
		jdbc.execute("ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payout_status VARCHAR(30) NOT NULL DEFAULT 'not_applicable'");
	}
}
