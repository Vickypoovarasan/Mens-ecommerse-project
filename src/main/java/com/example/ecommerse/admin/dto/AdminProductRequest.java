package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminProductRequest(
		@NotBlank @Size(max = 200) String name,
		@Size(max = 5000) String description,
		@Size(max = 100) String category,
		@NotNull @DecimalMin("0.01") BigDecimal basePrice,
		boolean active) {
}
