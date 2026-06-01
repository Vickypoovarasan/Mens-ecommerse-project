package com.example.ecommerse.catalog.dto;

import java.util.List;

public record ProductPageResponse(
		List<ProductSummaryResponse> items,
		int page,
		int size,
		long totalElements,
		int totalPages
) {
}
