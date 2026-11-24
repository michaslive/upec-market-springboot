package com.upecmarket.backend;

import java.math.BigDecimal;

import jakarta.validation.Validation;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.upecmarket.backend.dto.ProductRequest;
import com.upecmarket.backend.dto.RegisterRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTests {
	private static jakarta.validation.Validator validator;
	private static jakarta.validation.ValidatorFactory validatorFactory;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@Test
	void requiresInstitutionalEmailAndStrongEnoughPassword() {
		assertTrue(validator.validate(new RegisterRequest("student@upec.edu.ec", "password1", "Estudiante")).isEmpty());
		assertFalse(validator.validate(new RegisterRequest("student@example.com", "short", "Estudiante")).isEmpty());
	}

	@Test
	void rejectsNegativePriceAndInvalidProductType() {
		assertTrue(validator.validate(new ProductRequest("Libro", "Descripción", 2L,
				new BigDecimal("0.00"), "good", "sale", null)).isEmpty());
		assertFalse(validator.validate(new ProductRequest("Libro", "Descripción", 2L,
				new BigDecimal("-1.00"), "broken", "sale", null)).isEmpty());
	}
}
