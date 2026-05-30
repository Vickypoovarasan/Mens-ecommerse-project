package com.example.ecommerse.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPaymentRequest(
		@NotBlank @Size(max = 120) String sessionId) {
}
