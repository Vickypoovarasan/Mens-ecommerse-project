package com.example.ecommerse.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import com.example.ecommerse.review.dto.ReviewResponse;

public record ProductDetailResponse(
		Long id,
		String name,
		String description,
		String category,
		BigDecimal basePrice,
		String imageKey,
		String imageUrl,
		double averageRating,
		int reviewCount,
		List<ReviewResponse> reviews,
		boolean canReview,
		List<VariantResponse> variants
) {
}
