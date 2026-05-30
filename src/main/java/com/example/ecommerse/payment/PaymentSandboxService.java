package com.example.ecommerse.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.domain.Order;
import com.example.ecommerse.domain.PaymentLedger;
import com.example.ecommerse.payment.dto.PaymentStatusResponse;
import com.example.ecommerse.repo.OrderRepository;
import com.example.ecommerse.repo.PaymentLedgerRepository;

@Service
public class PaymentSandboxService {

	private static final String EVENT_CAPTURED = "payment.captured";

	private final PaymentLedgerRepository paymentLedgerRepository;
	private final OrderRepository orderRepository;

	@Value("${app.payment.webhook-secret:local-dev-webhook-secret}")
	private String webhookSecret;

	public PaymentSandboxService(PaymentLedgerRepository paymentLedgerRepository, OrderRepository orderRepository) {
		this.paymentLedgerRepository = paymentLedgerRepository;
		this.orderRepository = orderRepository;
	}

	@Transactional
	public PaymentStatusResponse confirmByUser(Long userId, String sessionId) {
		PaymentLedger ledger = loadPendingSession(sessionId);
		Order order = ledger.getOrder();
		if (!order.getUser().getId().equals(userId)) {
			throw new IllegalArgumentException("Payment session not found");
		}
		return capturePayment(ledger, order);
	}

	@Transactional
	public PaymentStatusResponse handleWebhook(String secret, String sessionId, String event) {
		if (secret == null || !secret.equals(webhookSecret)) {
			throw new IllegalArgumentException("Invalid webhook secret");
		}
		if (!EVENT_CAPTURED.equals(event)) {
			throw new IllegalArgumentException("Unsupported webhook event: " + event);
		}
		PaymentLedger ledger = loadPendingSession(sessionId);
		return capturePayment(ledger, ledger.getOrder());
	}

	private PaymentLedger loadPendingSession(String sessionId) {
		PaymentLedger ledger = paymentLedgerRepository.findByProviderRef(sessionId.trim())
				.orElseThrow(() -> new IllegalArgumentException("Payment session not found"));
		if (!"SANDBOX".equals(ledger.getProvider())) {
			throw new IllegalArgumentException("Not a sandbox payment session");
		}
		if ("CAPTURED".equals(ledger.getState())) {
			return ledger;
		}
		if (!"PENDING".equals(ledger.getState())) {
			throw new IllegalStateException("Payment is not pending");
		}
		return ledger;
	}

	private PaymentStatusResponse capturePayment(PaymentLedger ledger, Order order) {
		if ("CAPTURED".equals(ledger.getState()) && "PAID".equals(order.getPaymentStatus())) {
			return toResponse(order, ledger);
		}
		ledger.setState("CAPTURED");
		order.setPaymentStatus("PAID");
		paymentLedgerRepository.save(ledger);
		orderRepository.save(order);
		return toResponse(order, ledger);
	}

	private static PaymentStatusResponse toResponse(Order order, PaymentLedger ledger) {
		return new PaymentStatusResponse(
				order.getId(),
				order.getPaymentStatus(),
				ledger.getState(),
				ledger.getProvider());
	}
}
