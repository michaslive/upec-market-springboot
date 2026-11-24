package com.upecmarket.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
		@NotBlank @Size(max = 120) String fullName,
		@Size(max = 80) String payoutBank,
		@Size(max = 120) String payoutAccount,
		@Size(max = 120) String payoutHolder,
		@Size(max = 120) String payoutIdentifier) {
}
