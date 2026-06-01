package com.example.ecommerse.catalog.dto;

import java.math.BigDecimal;

public record VariantResponse(
		Long id,
		String size,
		String color,
		String sku,
		BigDecimal price,
		int stock,
		boolean inStock
) {
}
