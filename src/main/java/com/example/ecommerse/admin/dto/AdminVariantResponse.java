package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;

public record AdminVariantResponse(
		Long id,
		String sku,
		String size,
		String color,
		BigDecimal price,
		int stock,
		boolean active) {
}
