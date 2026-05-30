package com.example.ecommerse.order.dto;

import java.math.BigDecimal;

public record CheckoutResponse(
		Long orderId,
		String status,
		String paymentStatus,
		BigDecimal subtotal,
		BigDecimal tax,
		BigDecimal shipping,
		BigDecimal total,
		String expectedDeliveryDate,
		String paymentSessionId,
		boolean paymentPending,
		String shippingAddress,
		String promoCode,
		BigDecimal discount) {
}
