package com.upecmarket.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.upecmarket.backend.entity.ProductEntity;

public record ProductResponse(
		Long id,
		String title,
		String description,
		BigDecimal price,
		String conditionType,
		String listingType,
		String imageUrl,
		String status,
		LocalDateTime createdAt,
		Long categoryId,
		String category,
		Long sellerId,
		String sellerName,
		String sellerPhoto,
		Double sellerRating) {
	public static ProductResponse from(ProductEntity product) {
		return new ProductResponse(
				product.getId(),
				product.getTitle(),
				product.getDescription(),
				product.getPrice(),
				product.getConditionType(),
				product.getListingType(),
				product.getImageUrl(),
				product.getStatus(),
				product.getCreatedAt(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getSeller().getId(),
				product.getSeller().getFullName(),
				product.getSeller().getProfilePhotoUrl(),
				null);
	}
}
