package com.example.ecommerse.order;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.domain.Order;
import com.example.ecommerse.domain.OrderItem;
import com.example.ecommerse.domain.PaymentLedger;
import com.example.ecommerse.domain.ProductVariant;
import com.example.ecommerse.order.dto.OrderDetailResponse;
import com.example.ecommerse.order.dto.OrderItemResponse;
import com.example.ecommerse.order.dto.OrderSummaryResponse;
import com.example.ecommerse.order.dto.TrackingStepResponse;
import com.example.ecommerse.repo.OrderItemRepository;
import com.example.ecommerse.repo.OrderRepository;
import com.example.ecommerse.repo.PaymentLedgerRepository;
import com.example.ecommerse.repo.ProductVariantRepository;

@Service
public class OrderService {

	private static final Duration CANCEL_WINDOW = Duration.ofHours(24);
	private static final Duration RETURN_WINDOW = Duration.ofDays(7);
	private static final List<String> TRACKING_FLOW = List.of(
			"PLACED", "CONFIRMED", "PACKED", "SHIPPED", "DELIVERED");
	private static final Set<String> CANCELLABLE = Set.of("PLACED", "CONFIRMED");

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final PaymentLedgerRepository paymentLedgerRepository;
	private final ProductVariantRepository variantRepository;

	public OrderService(
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			PaymentLedgerRepository paymentLedgerRepository,
			ProductVariantRepository variantRepository) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.paymentLedgerRepository = paymentLedgerRepository;
		this.variantRepository = variantRepository;
	}

	@Transactional(readOnly = true)
	public List<OrderSummaryResponse> listOrders(Long userId) {
		return orderRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
				.map(this::toSummary)
				.toList();
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse getOrder(Long userId, Long orderId) {
		Order order = loadOwnedOrder(userId, orderId);
		return toDetail(order);
	}

	@Transactional
	public OrderDetailResponse cancelOrder(Long userId, Long orderId) {
		Order order = loadOwnedOrder(userId, orderId);
		if (!canCancel(order)) {
			throw new IllegalArgumentException(cancelMessage(order));
		}

		List<OrderItem> items = orderItemRepository.findByOrder_IdOrderByIdAsc(orderId);
		for (OrderItem item : items) {
			ProductVariant variant = variantRepository.findById(item.getVariant().getId())
					.orElseThrow(() -> new IllegalArgumentException("Variant not found"));
			variant.setStock(variant.getStock() + item.getQuantity());
			variantRepository.save(variant);
		}

		order.setStatus("CANCELLED");
		order.setPaymentStatus("REFUNDED");
		orderRepository.save(order);

		paymentLedgerRepository.findByOrder_Id(orderId).ifPresent(ledger -> {
			ledger.setState("REFUNDED");
			paymentLedgerRepository.save(ledger);
		});

		return toDetail(order);
	}

	@Transactional
	public OrderDetailResponse requestReturn(Long userId, Long orderId) {
		Order order = loadOwnedOrder(userId, orderId);
		if (!canReturn(order)) {
			throw new IllegalArgumentException(returnMessage(order));
		}
		order.setStatus("RETURN_REQUESTED");
		orderRepository.save(order);
		return toDetail(order);
	}

	private Order loadOwnedOrder(Long userId, Long orderId) {
		return orderRepository.findByIdAndUser_Id(orderId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
	}

	private OrderSummaryResponse toSummary(Order order) {
		int itemCount = orderItemRepository.countByOrder_Id(order.getId());
		return new OrderSummaryResponse(
				order.getId(),
				order.getStatus(),
				order.getPaymentStatus(),
				order.getTotal(),
				formatInstant(orderDate(order)),
				formatDate(order.getExpectedDeliveryDate()),
				itemCount);
	}

	private OrderDetailResponse toDetail(Order order) {
		List<OrderItem> items = orderItemRepository.findByOrder_IdOrderByIdAsc(order.getId());
		List<OrderItemResponse> itemResponses = items.stream().map(this::toItemResponse).toList();

		return new OrderDetailResponse(
				order.getId(),
				order.getStatus(),
				order.getPaymentStatus(),
				order.getPaymentMethod(),
				order.getShippingAddress(),
				order.getSubtotal(),
				order.getDiscount() != null ? order.getDiscount() : java.math.BigDecimal.ZERO,
				order.getPromoCode(),
				order.getTax(),
				order.getShipping(),
				order.getTotal(),
				formatInstant(orderDate(order)),
				formatDate(order.getExpectedDeliveryDate()),
				formatDate(order.getActualDeliveryDate()),
				itemResponses,
				buildTrackingSteps(order.getStatus()),
				canCancel(order),
				cancelMessage(order),
				canReturn(order),
				returnMessage(order));
	}

	private OrderItemResponse toItemResponse(OrderItem item) {
		return new OrderItemResponse(
				item.getId(),
				item.getProduct().getName(),
				item.getVariant().getSize(),
				item.getVariant().getColor(),
				item.getQuantity(),
				item.getUnitPrice(),
				item.getLineTotal());
	}

	private Instant orderDate(Order order) {
		return order.getOrderDate() != null ? order.getOrderDate() : order.getCreatedAt();
	}

	private boolean canCancel(Order order) {
		if (!CANCELLABLE.contains(order.getStatus())) {
			return false;
		}
		Instant placed = orderDate(order);
		if (placed == null) {
			return false;
		}
		return Instant.now().isBefore(placed.plus(CANCEL_WINDOW));
	}

	private boolean canReturn(Order order) {
		if (!"DELIVERED".equals(order.getStatus())) {
			return false;
		}
		LocalDate delivered = order.getActualDeliveryDate();
		if (delivered == null) {
			return false;
		}
		LocalDate lastDay = delivered.plusDays(RETURN_WINDOW.toDays());
		return !LocalDate.now(ZoneOffset.UTC).isAfter(lastDay);
	}

	private String cancelMessage(Order order) {
		if ("CANCELLED".equals(order.getStatus())) {
			return "Order is already cancelled.";
		}
		if (!CANCELLABLE.contains(order.getStatus())) {
			return "Cancel only while order is Placed or Confirmed (before shipping).";
		}
		Instant placed = orderDate(order);
		if (placed != null && !Instant.now().isBefore(placed.plus(CANCEL_WINDOW))) {
			return "Cancel window expired (24 hours from order time).";
		}
		return "You can cancel this order.";
	}

	private String returnMessage(Order order) {
		if ("RETURN_REQUESTED".equals(order.getStatus())) {
			return "Return already requested.";
		}
		if ("RETURNED".equals(order.getStatus())) {
			return "Order has been returned.";
		}
		if (!"DELIVERED".equals(order.getStatus())) {
			return "Returns only for Delivered orders.";
		}
		if (order.getActualDeliveryDate() == null) {
			return "Delivery date not recorded yet.";
		}
		if (!canReturn(order)) {
			return "Return window expired (7 days after delivery).";
		}
		return "You can request a return within 7 days of delivery.";
	}

	private List<TrackingStepResponse> buildTrackingSteps(String status) {
		if ("CANCELLED".equals(status)) {
			return List.of(new TrackingStepResponse("CANCELLED", "Cancelled", true, true));
		}
		if ("RETURN_REQUESTED".equals(status)) {
			return List.of(new TrackingStepResponse("RETURN_REQUESTED", "Return requested", true, true));
		}
		if ("RETURNED".equals(status)) {
			return List.of(new TrackingStepResponse("RETURNED", "Returned", true, true));
		}

		int current = TRACKING_FLOW.indexOf(status);
		if (current < 0) {
			current = 0;
		}

		List<TrackingStepResponse> steps = new ArrayList<>();
		for (int i = 0; i < TRACKING_FLOW.size(); i++) {
			String code = TRACKING_FLOW.get(i);
			steps.add(new TrackingStepResponse(
					code,
					trackingLabel(code),
					i < current,
					i == current));
		}
		return steps;
	}

	private static String trackingLabel(String code) {
		return switch (code) {
			case "PLACED" -> "Order placed";
			case "CONFIRMED" -> "Confirmed";
			case "PACKED" -> "Packed";
			case "SHIPPED" -> "Shipped";
			case "DELIVERED" -> "Delivered";
			default -> code;
		};
	}

	private static String formatInstant(Instant instant) {
		return instant == null ? null : instant.toString();
	}

	private static String formatDate(LocalDate date) {
		return date == null ? null : date.toString();
	}
}
