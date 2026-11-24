package com.upecmarket.backend;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.reservation.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationCleanup {
	private final JdbcTemplate jdbc;

	public ReservationCleanup(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Scheduled(fixedDelay = 30000)
	@Transactional
	public void expireReservations() throws Exception {
		try {
			if (jdbc.getDataSource() != null) {
				try (java.sql.Connection connection = jdbc.getDataSource().getConnection()) {
					String databaseProductName = connection.getMetaData().getDatabaseProductName().toLowerCase();
					if (databaseProductName.contains("postgresql")) {
						Boolean transactionsExist = jdbc.queryForObject(
								"SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'transactions')",
								Boolean.class);
						Boolean productsExist = jdbc.queryForObject(
								"SELECT EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'products')",
								Boolean.class);
						if (!Boolean.TRUE.equals(transactionsExist) || !Boolean.TRUE.equals(productsExist)) {
							return;
						}
					}
				}
			}
		} catch (org.springframework.dao.DataAccessException ignored) {
			return;
		}
		jdbc.update("""
			UPDATE transactions
			SET status = 'cancelled'
			WHERE status = 'pending_payment'
			  AND product_id IN (
			    SELECT id FROM products
			    WHERE status = 'reserved' AND reservation_expires_at <= CURRENT_TIMESTAMP
			  )
				""");
		jdbc.update("""
				UPDATE products SET status = 'approved', reserved_by = NULL, reservation_expires_at = NULL
				WHERE status = 'reserved' AND reservation_expires_at <= CURRENT_TIMESTAMP
				""");
		jdbc.update("""
			DELETE FROM cart_items
			WHERE product_id IN (
			  SELECT id FROM products WHERE status IN ('sold', 'exchanged', 'deleted')
			)
				""");
		jdbc.update("DELETE FROM auth_sessions WHERE expires_at <= CURRENT_TIMESTAMP");
	}
}
