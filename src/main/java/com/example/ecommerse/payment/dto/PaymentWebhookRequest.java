package com.example.ecommerse.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentWebhookRequest(
		@NotBlank @Size(max = 120) String sessionId,
		@NotBlank @Size(max = 50) String event) {
}
