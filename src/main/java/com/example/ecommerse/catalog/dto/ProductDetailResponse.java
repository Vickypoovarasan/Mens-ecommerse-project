package com.example.ecommerse.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

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
		List<VariantResponse> variants
) {
}
