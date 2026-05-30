package com.example.ecommerse.promo;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.promo.dto.PromoValidateRequest;
import com.example.ecommerse.promo.dto.PromoValidateResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/promos")
@Validated
public class PromoController {

	private final PromoCodeService promoCodeService;

	public PromoController(PromoCodeService promoCodeService) {
		this.promoCodeService = promoCodeService;
	}

	@PostMapping("/validate")
	public ResponseEntity<PromoValidateResponse> validate(@Valid @RequestBody PromoValidateRequest request) {
		return ResponseEntity.ok(promoCodeService.validate(request.code(), request.subtotal()));
	}
}
