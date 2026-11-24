package com.upecmarket.backend;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
	private static final String EMAIL_DOMAIN = "@upec.edu.ec";
	private final JdbcTemplate jdbc;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final SecureRandom secureRandom = new SecureRandom();
	private final long sessionExpirationHours;

	public AuthService(JdbcTemplate jdbc, @Value("${app.session.expiration-hours}") long sessionExpirationHours) {
		this.jdbc = jdbc;
		this.sessionExpirationHours = sessionExpirationHours;
	}

	@Transactional
	public Map<String, Object> register(Map<String, Object> body) {
		String email = requiredText(body, "email").toLowerCase();
		String password = requiredText(body, "password");
		String fullName = requiredText(body, "fullName");
		validateEmail(email);
		if (password.length() < 8 || password.length() > 72) {
			throw ApiException.badRequest("La contraseña debe tener entre 8 y 72 caracteres.");
		}
		if (fullName.length() > 120) {
			throw ApiException.badRequest("El nombre no puede superar 120 caracteres.");
		}
		if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email) > 0) {
			throw ApiException.conflict("Ya existe una cuenta con ese correo.");
		}

		KeyHolder keys = new GeneratedKeyHolder();
		jdbc.update(connection -> {
			var statement = connection.prepareStatement(
					"INSERT INTO users (email, password_hash, full_name) VALUES (?, ?, ?)",
					java.sql.Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, email);
			statement.setString(2, passwordEncoder.encode(password));
			statement.setString(3, fullName);
			return statement;
		}, keys);
		long userId = generatedId(keys);
		int assignedRoles = jdbc.update("""
				INSERT INTO user_roles (user_id, role_id)
				SELECT ?, id FROM roles WHERE name IN ('BUYER', 'SELLER')
				""", userId);
		if (assignedRoles != 2) {
			throw new IllegalStateException("Faltan los roles BUYER y SELLER en la base de datos.");
		}
		return issueSession(userId);
	}

	public Map<String, Object> login(Map<String, Object> body) {
		String email = requiredText(body, "email").toLowerCase();
		String password = requiredText(body, "password");
		List<Map<String, Object>> users = jdbc.queryForList(
				"SELECT id, email, full_name, password_hash, account_status FROM users WHERE email = ?", email);
		if (users.isEmpty() || !passwordEncoder.matches(password, (String) users.getFirst().get("password_hash"))) {
			throw ApiException.unauthorized("Correo o contraseña incorrectos.");
		}
		Map<String, Object> user = users.getFirst();
		if (!"active".equals(user.get("account_status"))) {
			throw ApiException.forbidden("La cuenta está restringida o suspendida.");
		}
		return issueSession(((Number) user.get("id")).longValue());
	}

	public AuthUser authenticate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return null;
		}
		List<Map<String, Object>> users = jdbc.queryForList("""
				SELECT u.id, u.email, u.full_name
				FROM auth_sessions s JOIN users u ON u.id = s.user_id
				WHERE s.token_hash = ? AND s.expires_at > CURRENT_TIMESTAMP AND u.account_status = 'active'
				""", hashToken(rawToken));
		if (users.isEmpty()) {
			return null;
		}
		Map<String, Object> user = users.getFirst();
		long userId = ((Number) user.get("id")).longValue();
		Set<String> roles = new HashSet<>(jdbc.queryForList("""
				SELECT r.name FROM user_roles ur JOIN roles r ON r.id = ur.role_id
				WHERE ur.user_id = ?
				""", String.class, userId));
		return new AuthUser(userId, (String) user.get("email"), (String) user.get("full_name"), Set.copyOf(roles));
	}

	public Map<String, Object> currentUser(AuthUser user) {
		Map<String, Object> profile = jdbc.queryForMap("""
				SELECT id, email, full_name AS fullName, profile_photo_url AS profilePhotoUrl,
				       account_status AS accountStatus, payout_bank AS payoutBank,
				       payout_account AS payoutAccount, payout_holder AS payoutHolder,
				       payout_identifier AS payoutIdentifier, created_at AS createdAt
				FROM users WHERE id = ?
				""", user.id());
		profile.put("roles", user.roles());
		return profile;
	}

	public void logout(String rawToken) {
		if (rawToken != null && !rawToken.isBlank()) {
			jdbc.update("DELETE FROM auth_sessions WHERE token_hash = ?", hashToken(rawToken));
		}
	}

	public boolean passwordMatches(long userId, String password) {
		if (password == null) {
			return false;
		}
		List<String> hashes = jdbc.queryForList("SELECT password_hash FROM users WHERE id = ?", String.class, userId);
		return !hashes.isEmpty() && passwordEncoder.matches(password, hashes.getFirst());
	}

	public void validateEmail(String email) {
		if (email.length() > 254 || !email.matches("^[A-Za-z0-9._%+-]+@upec\\.edu\\.ec$")) {
			throw ApiException.badRequest("Usa un correo institucional válido @upec.edu.ec.");
		}
	}

	public static String requiredText(Map<String, Object> body, String key) {
		Object value = body.get(key);
		if (!(value instanceof String text) || text.isBlank()) {
			throw ApiException.badRequest("El campo '" + key + "' es obligatorio.");
		}
		return text.trim();
	}

	private Map<String, Object> issueSession(long userId) {
		byte[] tokenBytes = new byte[32];
		secureRandom.nextBytes(tokenBytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		Instant expiresAt = Instant.now().plusSeconds(sessionExpirationHours * 3600);
		jdbc.update("INSERT INTO auth_sessions (user_id, token_hash, expires_at) VALUES (?, ?, ?)",
				userId, hashToken(token), Timestamp.from(expiresAt));
		return Map.of("token", token, "tokenType", "Bearer", "expiresAt", expiresAt.toString(),
				"user", currentUser(authenticate(token)));
	}

	private long generatedId(KeyHolder keys) {
		Number key = keys.getKey();
		if (key == null) {
			throw new IllegalStateException("La base de datos no devolvió el identificador de usuario.");
		}
		return key.longValue();
	}

	private static String hashToken(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 no está disponible.", exception);
		}
	}
}
