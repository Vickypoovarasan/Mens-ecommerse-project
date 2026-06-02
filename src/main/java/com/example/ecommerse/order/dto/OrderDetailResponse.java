package com.example.ecommerse.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailResponse(
		Long id,
		String status,
		String paymentStatus,
		String paymentMethod,
		String shippingAddress,
		BigDecimal subtotal,
		BigDecimal discount,
		String promoCode,
		BigDecimal tax,
		BigDecimal shipping,
		BigDecimal total,
		String orderDate,
		String expectedDeliveryDate,
		String actualDeliveryDate,
		List<OrderItemResponse> items,
		List<TrackingStepResponse> trackingSteps,
		boolean canCancel,
		String cancelMessage,
		boolean canReturn,
		String returnMessage,
		String returnReason) {
}
