package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;

public record AdminProductResponse(
		Long id,
		String name,
		String description,
		String category,
		BigDecimal basePrice,
		boolean active) {
}
