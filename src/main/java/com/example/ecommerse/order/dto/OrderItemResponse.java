package com.example.ecommerse.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long id,
		String productName,
		String size,
		String color,
		int quantity,
		BigDecimal unitPrice,
		BigDecimal lineTotal) {
}
