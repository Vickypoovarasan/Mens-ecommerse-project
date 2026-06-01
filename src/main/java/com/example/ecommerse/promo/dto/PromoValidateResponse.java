package com.example.ecommerse.promo.dto;

import java.math.BigDecimal;

public record PromoValidateResponse(
		boolean valid,
		String code,
		String discountType,
		BigDecimal discountAmount,
		String message) {
}
