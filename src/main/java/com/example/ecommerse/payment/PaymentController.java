package com.example.ecommerse.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.payment.dto.ConfirmPaymentRequest;
import com.example.ecommerse.payment.dto.PaymentStatusResponse;
import com.example.ecommerse.payment.dto.PaymentWebhookRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentController {

	private final PaymentSandboxService paymentSandboxService;

	public PaymentController(PaymentSandboxService paymentSandboxService) {
		this.paymentSandboxService = paymentSandboxService;
	}

	@PostMapping("/sandbox/confirm")
	public ResponseEntity<PaymentStatusResponse> confirmSandbox(
			@Valid @RequestBody ConfirmPaymentRequest request,
			Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		return ResponseEntity.ok(paymentSandboxService.confirmByUser(userId, request.sessionId()));
	}

	@PostMapping("/webhook/sandbox")
	public ResponseEntity<PaymentStatusResponse> sandboxWebhook(
			@RequestHeader(value = "X-Webhook-Secret", required = false) String secret,
			@Valid @RequestBody PaymentWebhookRequest request) {
		return ResponseEntity.ok(paymentSandboxService.handleWebhook(
				secret, request.sessionId(), request.event()));
	}
}
