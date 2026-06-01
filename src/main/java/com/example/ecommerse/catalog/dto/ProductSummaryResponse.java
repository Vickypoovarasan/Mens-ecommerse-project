package com.example.ecommerse.catalog.dto;

import java.math.BigDecimal;

public record ProductSummaryResponse(
		Long id,
		String name,
		String category,
		BigDecimal basePrice,
		String imageKey
) {
}
