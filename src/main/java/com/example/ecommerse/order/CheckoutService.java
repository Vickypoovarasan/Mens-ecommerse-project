package com.example.ecommerse.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.domain.IdempotencyKey;
import com.example.ecommerse.domain.Order;
import com.example.ecommerse.domain.OrderItem;
import com.example.ecommerse.domain.PaymentLedger;
import com.example.ecommerse.domain.ProductVariant;
import com.example.ecommerse.domain.User;
import com.example.ecommerse.order.dto.CheckoutItemRequest;
import com.example.ecommerse.order.dto.CheckoutRequest;
import com.example.ecommerse.order.dto.CheckoutResponse;
import com.example.ecommerse.promo.PromoCodeService;
import com.example.ecommerse.promo.PromoCodeService.PromoApplication;
import com.example.ecommerse.repo.IdempotencyKeyRepository;
import com.example.ecommerse.repo.OrderItemRepository;
import com.example.ecommerse.repo.OrderRepository;
import com.example.ecommerse.repo.PaymentLedgerRepository;
import com.example.ecommerse.repo.ProductVariantRepository;
import com.example.ecommerse.repo.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CheckoutService {

	private static final BigDecimal TAX_RATE = new BigDecimal("0.12");
	private static final BigDecimal SHIPPING_FLAT = new BigDecimal("15.00");
	private static final BigDecimal FREE_SHIPPING_MIN = new BigDecimal("150.00");
	private static final String STATUS_COMPLETED = "COMPLETED";
	private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

	private final UserRepository userRepository;
	private final ProductVariantRepository variantRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final PaymentLedgerRepository paymentLedgerRepository;
	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final PromoCodeService promoCodeService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public CheckoutService(
			UserRepository userRepository,
			ProductVariantRepository variantRepository,
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			PaymentLedgerRepository paymentLedgerRepository,
			IdempotencyKeyRepository idempotencyKeyRepository,
			PromoCodeService promoCodeService) {
		this.userRepository = userRepository;
		this.variantRepository = variantRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.paymentLedgerRepository = paymentLedgerRepository;
		this.idempotencyKeyRepository = idempotencyKeyRepository;
		this.promoCodeService = promoCodeService;
	}

	@Transactional
	public CheckoutResponse checkout(Long userId, String idempotencyKey, CheckoutRequest request) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new IllegalArgumentException("Idempotency-Key header is required");
		}
		String key = idempotencyKey.trim();
		if (key.length() > 80) {
			throw new IllegalArgumentException("Idempotency-Key too long");
		}

		byte[] requestHash = hashRequest(request);
		Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByUser_IdAndKey(userId, key);
		if (existing.isPresent()) {
			return handleExistingIdempotency(existing.get(), requestHash);
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		IdempotencyKey record = new IdempotencyKey();
		record.setUser(user);
		record.setKey(key);
		record.setRequestHash(requestHash);
		record.setStatus(STATUS_IN_PROGRESS);
		record.setExpiresAt(Instant.now().plusSeconds(86400));
		try {
			idempotencyKeyRepository.save(record);
		} catch (DataIntegrityViolationException ex) {
			IdempotencyKey raced = idempotencyKeyRepository.findByUser_IdAndKey(userId, key)
					.orElseThrow(() -> ex);
			return handleExistingIdempotency(raced, requestHash);
		}

		try {
			CheckoutResponse response = placeOrder(user, request);
			record.setStatus(STATUS_COMPLETED);
			record.setResponseJson(toJson(response));
			idempotencyKeyRepository.save(record);
			return response;
		} catch (RuntimeException ex) {
			idempotencyKeyRepository.delete(record);
			throw ex;
		}
	}

	private CheckoutResponse handleExistingIdempotency(IdempotencyKey record, byte[] requestHash) {
		if (!MessageDigest.isEqual(record.getRequestHash(), requestHash)) {
			throw new IllegalStateException("Idempotency-Key reused with different request body");
		}
		if (STATUS_COMPLETED.equals(record.getStatus()) && record.getResponseJson() != null) {
			return fromJson(record.getResponseJson());
		}
		throw new IllegalStateException("Checkout already in progress for this Idempotency-Key");
	}

	private CheckoutResponse placeOrder(User user, CheckoutRequest request) {
		Map<Long, Integer> qtyByVariant = mergeQuantities(request.items());
		if (qtyByVariant.isEmpty()) {
			throw new IllegalArgumentException("Cart is empty");
		}

		List<Line> lines = new ArrayList<>();
		for (Map.Entry<Long, Integer> entry : qtyByVariant.entrySet()) {
			ProductVariant variant = variantRepository.findByIdAndActiveTrue(entry.getKey())
					.orElseThrow(() -> new IllegalArgumentException("Variant not found: " + entry.getKey()));

			int qty = entry.getValue();
			if (variant.getStock() < qty) {
				throw new IllegalArgumentException("Insufficient stock for SKU " + variant.getSku());
			}

			variant.setStock(variant.getStock() - qty);
			try {
				variantRepository.save(variant);
			} catch (ObjectOptimisticLockingFailureException ex) {
				throw new IllegalStateException("Inventory conflict, please retry");
			}

			BigDecimal unitPrice = variant.getPrice();
			BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
			lines.add(new Line(variant, qty, unitPrice, lineTotal));
		}

		BigDecimal subtotal = lines.stream()
				.map(Line::lineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);

		PromoApplication promo = promoCodeService.resolveForCheckout(request.promoCode(), subtotal);
		BigDecimal discount = promo.discountAmount();
		BigDecimal discountedSubtotal = subtotal.subtract(discount).max(BigDecimal.ZERO)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal tax = discountedSubtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
		BigDecimal shipping = discountedSubtotal.compareTo(BigDecimal.ZERO) == 0
				|| discountedSubtotal.compareTo(FREE_SHIPPING_MIN) >= 0
						? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
						: SHIPPING_FLAT;
		BigDecimal total = discountedSubtotal.add(tax).add(shipping).setScale(2, RoundingMode.HALF_UP);

		String paymentMethod = normalizePaymentMethod(request.paymentMethod());
		LocalDate expectedDelivery = LocalDate.now().plusDays(5);
		String shippingAddress = resolveShippingAddress(user, request);

		Order order = new Order();
		order.setUser(user);
		order.setShippingAddress(shippingAddress);
		order.setStatus("PLACED");
		order.setPaymentMethod(paymentMethod);
		order.setSubtotal(subtotal);
		order.setDiscount(discount);
		order.setPromoCode(promo.code());
		order.setTax(tax);
		order.setShipping(shipping);
		order.setTotal(total);
		order.setExpectedDeliveryDate(expectedDelivery);
		order = orderRepository.save(order);
		if (promo.applied()) {
			promoCodeService.recordUsage(promo.code());
		}

		for (Line line : lines) {
			OrderItem item = new OrderItem();
			item.setOrder(order);
			item.setProduct(line.variant().getProduct());
			item.setVariant(line.variant());
			item.setQuantity(line.quantity());
			item.setUnitPrice(line.unitPrice());
			item.setLineTotal(line.lineTotal());
			orderItemRepository.save(item);
		}

		String paymentSessionId = null;
		boolean paymentPending = false;

		PaymentLedger ledger = new PaymentLedger();
		ledger.setOrder(order);
		ledger.setAmount(total);

		if ("CARD".equals(paymentMethod)) {
			paymentSessionId = UUID.randomUUID().toString();
			paymentPending = true;
			order.setPaymentStatus("PENDING");
			ledger.setProvider("SANDBOX");
			ledger.setProviderRef(paymentSessionId);
			ledger.setState("PENDING");
		} else {
			order.setPaymentStatus("PAID");
			ledger.setProvider("COD");
			ledger.setProviderRef("COD-" + order.getId());
			ledger.setState("CAPTURED");
		}
		paymentLedgerRepository.save(ledger);

		return new CheckoutResponse(
				order.getId(),
				order.getStatus(),
				order.getPaymentStatus(),
				subtotal,
				tax,
				shipping,
				total,
				expectedDelivery.toString(),
				paymentSessionId,
				paymentPending,
				shippingAddress,
				promo.code(),
				discount);
	}

	private String resolveShippingAddress(User user, CheckoutRequest request) {
		String fromRequest = request.shippingAddress() == null ? "" : request.shippingAddress().trim();
		String resolved = fromRequest;
		if (resolved.isBlank()) {
			String profile = user.getAddress();
			if (profile != null && !profile.isBlank()) {
				resolved = profile.trim();
			}
		}
		if (resolved.isBlank()) {
			throw new IllegalArgumentException("Shipping address is required");
		}
		if (request.saveToProfile() && !fromRequest.isBlank()) {
			user.setAddress(resolved);
			userRepository.save(user);
		}
		return resolved;
	}

	private static Map<Long, Integer> mergeQuantities(List<CheckoutItemRequest> items) {
		Map<Long, Integer> merged = new LinkedHashMap<>();
		List<CheckoutItemRequest> sorted = items.stream()
				.sorted(Comparator.comparing(CheckoutItemRequest::variantId))
				.toList();
		for (CheckoutItemRequest item : sorted) {
			merged.merge(item.variantId(), item.quantity(), Integer::sum);
		}
		return merged;
	}

	private static String normalizePaymentMethod(String method) {
		String m = method.trim().toUpperCase();
		if (!m.equals("CARD") && !m.equals("COD")) {
			throw new IllegalArgumentException("paymentMethod must be CARD or COD");
		}
		return m;
	}

	private byte[] hashRequest(CheckoutRequest request) {
		try {
			List<CheckoutItemRequest> sorted = request.items().stream()
					.sorted(Comparator.comparing(CheckoutItemRequest::variantId))
					.toList();
			String promo = request.promoCode() == null ? "" : request.promoCode().trim().toUpperCase();
			String canonical = objectMapper.writeValueAsString(new CheckoutRequest(
					request.paymentMethod().trim().toUpperCase(),
					sorted,
					request.shippingAddress() == null ? "" : request.shippingAddress().trim(),
					request.saveToProfile(),
					promo));
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid checkout request");
		}
	}

	private String toJson(CheckoutResponse response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize checkout response");
		}
	}

	private CheckoutResponse fromJson(String json) {
		try {
			return objectMapper.readValue(json, CheckoutResponse.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Stored idempotency response is invalid");
		}
	}

	private record Line(ProductVariant variant, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
	}
}
