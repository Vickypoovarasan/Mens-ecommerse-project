package com.example.ecommerse.order.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
		@NotBlank @Size(max = 30) String paymentMethod,
		@NotEmpty @Valid List<CheckoutItemRequest> items,
		@Size(max = 500) String shippingAddress,
		boolean saveToProfile,
		@Size(max = 40) String promoCode) {
}
