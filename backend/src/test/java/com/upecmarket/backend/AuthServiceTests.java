package com.upecmarket.backend;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTests {
	private final AuthService authService = new AuthService(new JdbcTemplate(), 24);

	@Test
	void acceptsInstitutionalEmail() {
		assertDoesNotThrow(() -> authService.validateEmail("student@upec.edu.ec"));
	}

	@Test
	void rejectsLookalikeAndNonInstitutionalDomains() {
		assertThrows(ApiException.class, () -> authService.validateEmail("student@upec.edu.ec.example.com"));
		assertThrows(ApiException.class, () -> authService.validateEmail("student@example.com"));
	}
}
