package com.example.ecommerse.order;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.order.dto.CheckoutRequest;
import com.example.ecommerse.order.dto.CheckoutResponse;
import com.example.ecommerse.order.dto.OrderDetailResponse;
import com.example.ecommerse.order.dto.OrderSummaryResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

	private final CheckoutService checkoutService;
	private final OrderService orderService;

	public OrderController(CheckoutService checkoutService, OrderService orderService) {
		this.checkoutService = checkoutService;
		this.orderService = orderService;
	}

	@GetMapping
	public ResponseEntity<List<OrderSummaryResponse>> list(Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		return ResponseEntity.ok(orderService.listOrders(userId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderDetailResponse> get(@PathVariable Long id, Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		return ResponseEntity.ok(orderService.getOrder(userId, id));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<OrderDetailResponse> cancel(@PathVariable Long id, Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		return ResponseEntity.ok(orderService.cancelOrder(userId, id));
	}

	@PostMapping("/{id}/return")
	public ResponseEntity<OrderDetailResponse> requestReturn(@PathVariable Long id, Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		return ResponseEntity.ok(orderService.requestReturn(userId, id));
	}

	@PostMapping("/checkout")
	public ResponseEntity<CheckoutResponse> checkout(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody CheckoutRequest request,
			Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		return ResponseEntity.ok(checkoutService.checkout(userId, idempotencyKey, request));
	}
}
