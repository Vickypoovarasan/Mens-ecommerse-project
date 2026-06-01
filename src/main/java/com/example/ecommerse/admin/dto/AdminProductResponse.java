package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;

public record AdminProductResponse(
		Long id,
		String name,
		String description,
		String category,
		String imageUrl,
		BigDecimal basePrice,
		boolean active) {
}
