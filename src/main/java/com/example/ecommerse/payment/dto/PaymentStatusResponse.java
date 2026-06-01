package com.example.ecommerse.payment.dto;

public record PaymentStatusResponse(
		Long orderId,
		String paymentStatus,
		String ledgerState,
		String provider) {
}
