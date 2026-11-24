package com.upecmarket.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email @Size(max = 254)
		@Pattern(regexp = "^[A-Za-z0-9._%+-]+@upec\\.edu\\.ec$", flags = Pattern.Flag.CASE_INSENSITIVE)
		String email,
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotBlank @Size(max = 120) String fullName) {
}
