package com.example.ecommerse.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminPromoResponse(
        Long id,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minSubtotal,
        Integer maxUses,
        int usedCount,
        boolean active,
        Instant validFrom,
        Instant validUntil) {
}
