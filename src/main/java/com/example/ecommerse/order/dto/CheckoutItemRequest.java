package com.example.ecommerse.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItemRequest(
		@NotNull Long variantId,
		@NotNull @Min(1) Integer quantity) {
}
