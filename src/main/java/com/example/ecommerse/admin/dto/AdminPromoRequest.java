package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPromoRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 20) String discountType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal discountValue,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal minSubtotal,
        Integer maxUses,
        boolean active,
        Instant validFrom,
        Instant validUntil) {
}
