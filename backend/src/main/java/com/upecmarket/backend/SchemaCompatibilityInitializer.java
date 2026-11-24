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
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_bank VARCHAR(80)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_account VARCHAR(120)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_holder VARCHAR(120)");
				jdbc.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS payout_identifier VARCHAR(120)");
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
