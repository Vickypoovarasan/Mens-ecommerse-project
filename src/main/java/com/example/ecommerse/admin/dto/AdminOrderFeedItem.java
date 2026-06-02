package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;

public record AdminOrderFeedItem(
		Long id,
		String customerEmail,
		String status,
		BigDecimal total,
		String orderDate,
		int itemCount,
		String actualDeliveryDate,
		String returnReason) {
}
