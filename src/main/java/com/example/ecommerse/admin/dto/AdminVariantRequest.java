package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminVariantRequest(
		@NotBlank @Size(max = 10) String size,
		@NotBlank @Size(max = 50) String color,
		@NotBlank @Size(max = 80) String sku,
		@NotNull @DecimalMin("0.01") BigDecimal price,
		@Min(0) int stock,
		boolean active) {
}
