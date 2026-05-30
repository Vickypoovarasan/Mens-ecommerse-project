package com.example.ecommerse.promo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.domain.PromoCode;
import com.example.ecommerse.promo.dto.PromoValidateResponse;
import com.example.ecommerse.repo.PromoCodeRepository;

@Service
public class PromoCodeService {

	private final PromoCodeRepository promoCodeRepository;

	public PromoCodeService(PromoCodeRepository promoCodeRepository) {
		this.promoCodeRepository = promoCodeRepository;
	}

	public PromoValidateResponse validate(String rawCode, BigDecimal subtotal) {
		String normalized = normalizeCode(rawCode);
		if (normalized.isEmpty()) {
			return new PromoValidateResponse(false, null, null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
					"Promo code is required");
		}

		Optional<PromoCode> promoOpt = promoCodeRepository.findByCodeAndActiveTrue(normalized);
		if (promoOpt.isEmpty()) {
			return invalid(normalized, "Promo code not found");
		}

		PromoCode promo = promoOpt.get();
		String error = eligibilityError(promo, subtotal);
		if (error != null) {
			return invalid(normalized, error);
		}

		BigDecimal discount = calculateDiscount(promo, subtotal);
		return new PromoValidateResponse(true, promo.getCode(), promo.getDiscountType(), discount, null);
	}

	public PromoApplication resolveForCheckout(String rawCode, BigDecimal subtotal) {
		if (rawCode == null || rawCode.isBlank()) {
			return PromoApplication.none();
		}

		PromoValidateResponse validation = validate(rawCode, subtotal);
		if (!validation.valid()) {
			throw new IllegalArgumentException(validation.message());
		}

		return new PromoApplication(validation.code(), validation.discountAmount());
	}

	@Transactional
	public void recordUsage(String code) {
		if (code == null || code.isBlank()) {
			return;
		}
		PromoCode promo = promoCodeRepository.findByCodeAndActiveTrue(normalizeCode(code))
				.orElseThrow(() -> new IllegalArgumentException("Promo code not found"));
		int updated = promoCodeRepository.incrementUsedCount(promo.getId());
		if (updated == 0) {
			throw new IllegalStateException("Promo code has reached its usage limit");
		}
	}

	public static BigDecimal calculateDiscount(PromoCode promo, BigDecimal subtotal) {
		BigDecimal base = subtotal.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
		BigDecimal discount;
		if ("PERCENT".equals(promo.getDiscountType())) {
			discount = base.multiply(promo.getDiscountValue())
					.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
		} else if ("FIXED".equals(promo.getDiscountType())) {
			discount = promo.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
		} else {
			throw new IllegalArgumentException("Unsupported promo discount type");
		}
		return discount.min(base).setScale(2, RoundingMode.HALF_UP);
	}

	private static String eligibilityError(PromoCode promo, BigDecimal subtotal) {
		Instant now = Instant.now();
		if (promo.getValidFrom() != null && now.isBefore(promo.getValidFrom())) {
			return "Promo code is not active yet";
		}
		if (promo.getValidUntil() != null && now.isAfter(promo.getValidUntil())) {
			return "Promo code has expired";
		}
		if (promo.getMaxUses() != null && promo.getUsedCount() >= promo.getMaxUses()) {
			return "Promo code has reached its usage limit";
		}
		if (promo.getMinSubtotal() != null
				&& subtotal.compareTo(promo.getMinSubtotal()) < 0) {
			return "Order subtotal does not meet the minimum for this promo";
		}
		return null;
	}

	private static PromoValidateResponse invalid(String code, String message) {
		return new PromoValidateResponse(false, code, null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), message);
	}

	public static String normalizeCode(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.trim().toUpperCase();
	}

	public record PromoApplication(String code, BigDecimal discountAmount) {

		public static PromoApplication none() {
			return new PromoApplication(null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
		}

		public boolean applied() {
			return code != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
		}
	}
}
