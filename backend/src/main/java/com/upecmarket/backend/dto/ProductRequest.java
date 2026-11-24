package com.upecmarket.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@NotBlank @Size(max = 160) String title,
		@NotBlank @Size(max = 5000) String description,
		@NotNull Long categoryId,
		@NotNull @DecimalMin("0.00") BigDecimal price,
		@Pattern(regexp = "new|like_new|good|fair") String conditionType,
		@Pattern(regexp = "sale|exchange|both") String listingType,
		@Size(max = 500) String imageUrl) {
	public String normalizedConditionType() {
		return conditionType == null ? "good" : conditionType;
	}

	public String normalizedListingType() {
		return listingType == null ? "sale" : listingType;
	}
}
