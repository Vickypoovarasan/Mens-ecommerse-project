package com.example.ecommerse.order.dto;

import java.math.BigDecimal;

public record OrderSummaryResponse(
		Long id,
		String status,
		String paymentStatus,
		BigDecimal total,
		String orderDate,
		String expectedDeliveryDate,
		int itemCount) {
}
