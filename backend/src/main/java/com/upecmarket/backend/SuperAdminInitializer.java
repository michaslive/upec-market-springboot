package com.upecmarket.backend;

import java.sql.Statement;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SuperAdminInitializer implements ApplicationRunner {
	private final JdbcTemplate jdbc;
	private final String email;
	private final String password;
	private final String fullName;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public SuperAdminInitializer(JdbcTemplate jdbc,
			@Value("${app.super-admin.email}") String email,
			@Value("${app.super-admin.password}") String password,
			@Value("${app.super-admin.name}") String fullName) {
		this.jdbc = jdbc;
		this.email = email;
		this.password = password;
		this.fullName = fullName;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (email.isBlank() && password.isBlank()) return;
		if (email.isBlank() || password.length() < 12 || password.length() > 72
				|| fullName.isBlank() || fullName.length() > 120) {
			throw new IllegalStateException(
					"Configura SUPER_ADMIN_EMAIL, una SUPER_ADMIN_PASSWORD de 12 a 72 caracteres y un nombre de hasta 120 caracteres.");
		}
		if (!email.matches("(?i)^[A-Za-z0-9._%+-]+@upec\\.edu\\.ec$")) {
			throw new IllegalStateException("SUPER_ADMIN_EMAIL debe usar el dominio @upec.edu.ec.");
		}
		List<java.util.Map<String, Object>> existingUsers = jdbc.queryForList(
				"SELECT id, password_hash, account_status FROM users WHERE email = ?", email.toLowerCase());
		long userId;
		if (existingUsers.isEmpty()) {
			KeyHolder keys = new GeneratedKeyHolder();
			jdbc.update(connection -> {
				var statement = connection.prepareStatement("""
						INSERT INTO users (email, password_hash, full_name)
						VALUES (?, ?, ?)
						""", Statement.RETURN_GENERATED_KEYS);
				statement.setString(1, email.toLowerCase());
				statement.setString(2, passwordEncoder.encode(password));
				statement.setString(3, fullName);
				return statement;
			}, keys);
			Number id = keys.getKey();
			if (id == null) throw new IllegalStateException("No se pudo crear el usuario Super Admin.");
			userId = id.longValue();
		} else {
			java.util.Map<String, Object> existingUser = existingUsers.getFirst();
			if (!passwordEncoder.matches(password, (String) existingUser.get("password_hash"))) {
				throw new IllegalStateException(
						"SUPER_ADMIN_EMAIL ya existe: confirma su contraseña actual antes de asignar el rol privilegiado.");
			}
			if (!"active".equals(existingUser.get("account_status"))) {
				throw new IllegalStateException("La cuenta indicada para Super Admin no está activa.");
			}
			userId = ((Number) existingUser.get("id")).longValue();
		}
		jdbc.update("""
				INSERT INTO user_roles (user_id, role_id)
				SELECT ?, id FROM roles WHERE name = 'SUPER_ADMIN'
			ON CONFLICT (user_id, role_id) DO UPDATE SET user_id = EXCLUDED.user_id
				""", userId);
	}
}
