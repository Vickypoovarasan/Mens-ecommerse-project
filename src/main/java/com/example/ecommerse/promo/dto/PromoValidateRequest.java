package com.example.ecommerse.promo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromoValidateRequest(
		@NotBlank @Size(max = 40) String code,
		@NotNull @DecimalMin("0.00") BigDecimal subtotal) {
}
