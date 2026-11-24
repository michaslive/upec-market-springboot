package com.upecmarket.backend;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
	private static final Map<String, String[]> DEMO_USERS = Map.of(
			"comprador@upec.edu.ec", new String[] {"Comprador Demo", "BUYER"},
			"vendedor@upec.edu.ec", new String[] {"Vendedor Demo", "SELLER"},
			"administrador@upec.edu.ec", new String[] {"Administrador Demo", "ADMIN"},
			"superadmin@upec.edu.ec", new String[] {"Super Administrador Demo", "SUPER_ADMIN"});

	private final JdbcTemplate jdbc;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final String password;

	public DemoDataInitializer(JdbcTemplate jdbc, @Value("${app.demo.password}") String password) {
		this.jdbc = jdbc;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		ensureRoles();

		for (Map.Entry<String, String[]> entry : DEMO_USERS.entrySet()) {
			String email = entry.getKey();
			String fullName = entry.getValue()[0];
			String role = entry.getValue()[1];
			ensureUser(email, fullName, role);
		}

		ensureSellerProducts();
	}

	private void ensureRoles() {
		jdbc.update("INSERT INTO roles (name) SELECT 'BUYER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'BUYER')");
		jdbc.update("INSERT INTO roles (name) SELECT 'SELLER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SELLER')");
		jdbc.update("INSERT INTO roles (name) SELECT 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN')");
		jdbc.update("INSERT INTO roles (name) SELECT 'SUPER_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'SUPER_ADMIN')");
	}

	private void ensureUser(String email, String fullName, String role) {
		jdbc.update("""
				INSERT INTO users (email, password_hash, password, full_name, name, account_status)
				SELECT ?, ?, ?, ?, ?, 'active'
				WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = ?)
				""", email, passwordEncoder.encode(password), passwordEncoder.encode(password), fullName, fullName, email);
		jdbc.update("""
				INSERT INTO user_roles (user_id, role_id)
				SELECT u.id, r.id FROM users u, roles r
				WHERE u.email = ? AND r.name = ?
				  AND NOT EXISTS (
				    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id
				  )
				""", email, role);
	}

	private void ensureSellerProducts() {
		Number sellerId = jdbc.queryForObject(
				"SELECT id FROM users WHERE email = 'vendedor@upec.edu.ec'", Number.class);

		seedProduct(sellerId.longValue(), "tecnologia", "Laptop ASUS VivoBook",
				"Laptop en buen estado, 16 GB RAM y SSD de 512 GB.", "650.00", "like_new", "sale");
		seedProduct(sellerId.longValue(), "libros", "Libro de Arquitectura de Software",
				"Libro universitario con notas mínimas, ideal para ingeniería.", "18.50", "good", "sale");
		seedProduct(sellerId.longValue(), "ropa", "Chaqueta universitaria",
				"Chaqueta azul talla M, poco uso y muy conservada.", "25.00", "like_new", "sale");
		seedProduct(sellerId.longValue(), "hogar", "Lámpara LED de escritorio",
				"Lámpara regulable con puerto USB para estudio.", "12.00", "good", "both");
		seedProduct(sellerId.longValue(), "deportes", "Balón profesional",
				"Balón de fútbol talla oficial, usado una temporada.", "20.00", "good", "exchange");
		seedProduct(sellerId.longValue(), "otros", "Calculadora científica",
				"Calculadora científica completa para clases de cálculo.", "30.00", "like_new", "both");
	}

	private void seedProduct(long sellerId, String slug, String title, String description,
			String price, String condition, String listingType) {
		jdbc.update("""
				INSERT INTO products (
					seller_id, category_id, title, description, price,
					condition_type, listing_type, status, deleted_at
				)
				SELECT ?, c.id, ?, ?, CAST(? AS DECIMAL(10,2)), ?, ?, 'approved', NULL
				FROM categories c
				WHERE c.slug = ?
				  AND NOT EXISTS (SELECT 1 FROM products p WHERE p.title = ? AND p.seller_id = ?)
				""", sellerId, title, description, price, condition, listingType, slug, title, sellerId);
	}
}
