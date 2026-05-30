package com.example.ecommerse.admin.dto;

import jakarta.validation.constraints.Min;

public record AdminVariantStockRequest(
		@Min(0) int stock) {
}
