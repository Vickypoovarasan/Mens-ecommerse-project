package com.example.ecommerse.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.admin.dto.AdminOrderFeedItem;
import com.example.ecommerse.admin.dto.AdminProductRequest;
import com.example.ecommerse.admin.dto.AdminProductResponse;
import com.example.ecommerse.admin.dto.AdminPromoRequest;
import com.example.ecommerse.admin.dto.AdminPromoResponse;
import com.example.ecommerse.admin.dto.AdminVariantRequest;
import com.example.ecommerse.admin.dto.AdminVariantResponse;
import com.example.ecommerse.admin.dto.UpdateOrderStatusRequest;
import com.example.ecommerse.domain.Order;
import com.example.ecommerse.domain.OrderItem;
import com.example.ecommerse.domain.PromoCode;
import com.example.ecommerse.domain.PaymentLedger;
import com.example.ecommerse.domain.Product;
import com.example.ecommerse.domain.ProductVariant;
import com.example.ecommerse.repo.OrderItemRepository;
import com.example.ecommerse.repo.OrderRepository;
import com.example.ecommerse.repo.PaymentLedgerRepository;
import com.example.ecommerse.repo.PromoCodeRepository;
import com.example.ecommerse.repo.ProductRepository;
import com.example.ecommerse.repo.ProductVariantRepository;

@Service
public class AdminService {

	private static final List<String> DISPATCH_STATUSES = List.of(
			"PLACED", "CONFIRMED", "PACKED", "SHIPPED");
	private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
			"PLACED", Set.of("CONFIRMED", "CANCELLED"),
			"CONFIRMED", Set.of("PACKED", "CANCELLED"),
			"PACKED", Set.of("SHIPPED"),
			"SHIPPED", Set.of("DELIVERED"));

	private final ProductRepository productRepository;
	private final ProductVariantRepository variantRepository;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final PaymentLedgerRepository paymentLedgerRepository;
	private final PromoCodeRepository promoCodeRepository;

	public AdminService(
			ProductRepository productRepository,
			ProductVariantRepository variantRepository,
			OrderRepository orderRepository,
			OrderItemRepository orderItemRepository,
			PaymentLedgerRepository paymentLedgerRepository,
			PromoCodeRepository promoCodeRepository) {
		this.productRepository = productRepository;
		this.variantRepository = variantRepository;
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.paymentLedgerRepository = paymentLedgerRepository;
		this.promoCodeRepository = promoCodeRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminProductResponse> listProducts() {
		return productRepository.findAllByOrderByNameAsc().stream()
				.map(this::toProductResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminPromoResponse> listPromos() {
		return promoCodeRepository.findAll().stream()
				.map(this::toPromoResponse)
				.toList();
	}

	@Transactional
	public AdminPromoResponse createPromo(AdminPromoRequest request) {
		String normalizedCode = request.code().trim().toUpperCase();
		if (promoCodeRepository.existsByCode(normalizedCode)) {
			throw new IllegalArgumentException("Promo code already exists: " + normalizedCode);
		}
		if (request.validFrom() != null && request.validUntil() != null && request.validFrom().isAfter(request.validUntil())) {
			throw new IllegalArgumentException("Promo valid from date must come before valid until date");
		}

		PromoCode promo = new PromoCode();
		promo.setCode(normalizedCode);
		promo.setDiscountType(request.discountType().trim().toUpperCase());
		promo.setDiscountValue(request.discountValue());
		promo.setMinSubtotal(request.minSubtotal());
		promo.setMaxUses(request.maxUses());
		promo.setUsedCount(0);
		promo.setActive(request.active());
		promo.setValidFrom(request.validFrom());
		promo.setValidUntil(request.validUntil());

		return toPromoResponse(promoCodeRepository.save(promo));
	}

	@Transactional
	public AdminPromoResponse updatePromo(Long id, AdminPromoRequest request) {
		PromoCode promo = promoCodeRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Promo code not found"));
		String normalizedCode = request.code().trim().toUpperCase();
		if (!normalizedCode.equals(promo.getCode()) && promoCodeRepository.existsByCode(normalizedCode)) {
			throw new IllegalArgumentException("Promo code already exists: " + normalizedCode);
		}
		if (request.validFrom() != null && request.validUntil() != null && request.validFrom().isAfter(request.validUntil())) {
			throw new IllegalArgumentException("Promo valid from date must come before valid until date");
		}

		promo.setCode(normalizedCode);
		promo.setDiscountType(request.discountType().trim().toUpperCase());
		promo.setDiscountValue(request.discountValue());
		promo.setMinSubtotal(request.minSubtotal());
		promo.setMaxUses(request.maxUses());
		promo.setActive(request.active());
		promo.setValidFrom(request.validFrom());
		promo.setValidUntil(request.validUntil());

		return toPromoResponse(promoCodeRepository.save(promo));
	}

	@Transactional
	public AdminProductResponse createProduct(AdminProductRequest request) {
		Product product = new Product();
		applyProduct(product, request);
		return toProductResponse(productRepository.save(product));
	}

	@Transactional
	public AdminProductResponse updateProduct(Long id, AdminProductRequest request) {
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		applyProduct(product, request);
		return toProductResponse(productRepository.save(product));
	}

	@Transactional(readOnly = true)
	public List<AdminVariantResponse> listVariants(Long productId) {
		if (!productRepository.existsById(productId)) {
			throw new IllegalArgumentException("Product not found");
		}
		return variantRepository.findByProductIdOrderBySizeAsc(productId).stream()
				.map(this::toVariantResponse)
				.toList();
	}

	@Transactional
	public AdminVariantResponse createVariant(Long productId, AdminVariantRequest request) {
		Product product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));

		String size = request.size().trim();
		String color = request.color().trim();
		String sku = request.sku().trim().toUpperCase();

		ensureSkuAvailable(sku, null);
		ensureSizeColorAvailable(product.getId(), size, color, null);

		ProductVariant variant = new ProductVariant();
		variant.setProduct(product);
		applyVariant(variant, request, size, color, sku);
		return toVariantResponse(variantRepository.save(variant));
	}

	@Transactional
	public AdminVariantResponse updateVariant(Long variantId, AdminVariantRequest request) {
		ProductVariant variant = variantRepository.findById(variantId)
				.orElseThrow(() -> new IllegalArgumentException("Variant not found"));

		String size = request.size().trim();
		String color = request.color().trim();
		String sku = request.sku().trim().toUpperCase();

		ensureSkuAvailable(sku, variant.getId());
		ensureSizeColorAvailable(variant.getProduct().getId(), size, color, variant.getId());

		applyVariant(variant, request, size, color, sku);
		return toVariantResponse(variantRepository.save(variant));
	}

	@Transactional
	public void updateVariantStock(Long variantId, int stock) {
		ProductVariant variant = variantRepository.findById(variantId)
				.orElseThrow(() -> new IllegalArgumentException("Variant not found"));
		variant.setStock(stock);
		variantRepository.save(variant);
	}

	private void applyVariant(ProductVariant variant, AdminVariantRequest request, String size, String color, String sku) {
		variant.setSize(size);
		variant.setColor(color);
		variant.setSku(sku);
		variant.setPrice(request.price());
		variant.setStock(request.stock());
		variant.setActive(request.active());
	}

	private void ensureSkuAvailable(String sku, Long excludeId) {
		boolean taken = excludeId == null
				? variantRepository.existsBySku(sku)
				: variantRepository.existsBySkuAndIdNot(sku, excludeId);
		if (taken) {
			throw new IllegalArgumentException("SKU already in use: " + sku);
		}
	}

	private void ensureSizeColorAvailable(Long productId, String size, String color, Long excludeId) {
		boolean taken = excludeId == null
				? variantRepository.existsByProduct_IdAndSizeAndColor(productId, size, color)
				: variantRepository.existsByProduct_IdAndSizeAndColorAndIdNot(productId, size, color, excludeId);
		if (taken) {
			throw new IllegalArgumentException("Variant already exists for size " + size + " and color " + color);
		}
	}

	private AdminVariantResponse toVariantResponse(ProductVariant variant) {
		return new AdminVariantResponse(
				variant.getId(),
				variant.getSku(),
				variant.getSize(),
				variant.getColor(),
				variant.getPrice(),
				variant.getStock(),
				variant.isActive());
	}

	@Transactional(readOnly = true)
	public List<AdminOrderFeedItem> dispatchFeed() {
		return orderRepository.findByStatusInOrderByCreatedAtDesc(DISPATCH_STATUSES).stream()
				.map(this::toFeedItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminOrderFeedItem> returnQueue() {
		return orderRepository.findByStatusOrderByCreatedAtDesc("RETURN_REQUESTED").stream()
				.map(this::toFeedItem)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AdminOrderFeedItem> completedOrders(LocalDate from, LocalDate to) {
		return orderRepository.findByStatusOrderByCreatedAtDesc("DELIVERED").stream()
				.filter(order -> {
					LocalDate delivered = order.getActualDeliveryDate();
					if (delivered == null) {
						return false;
					}
					if (from != null && delivered.isBefore(from)) {
						return false;
					}
					if (to != null && delivered.isAfter(to)) {
						return false;
					}
					return true;
				})
				.map(this::toFeedItem)
				.toList();
	}

	@Transactional
	public AdminOrderFeedItem updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));

		String target = request.status().trim().toUpperCase();
		String current = order.getStatus();

		if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
			throw new IllegalArgumentException(
					"Cannot move order from " + current + " to " + target);
		}

		if ("CANCELLED".equals(target)) {
			restoreStock(orderId);
			order.setPaymentStatus("REFUNDED");
			paymentLedgerRepository.findByOrder_Id(orderId).ifPresent(ledger -> {
				ledger.setState("REFUNDED");
				paymentLedgerRepository.save(ledger);
			});
		}

		order.setStatus(target);
		if ("DELIVERED".equals(target)) {
			order.setActualDeliveryDate(LocalDate.now(ZoneOffset.UTC));
		}
		orderRepository.save(order);
		return toFeedItem(order);
	}

	@Transactional
	public AdminOrderFeedItem approveReturn(Long orderId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));

		if (!"RETURN_REQUESTED".equals(order.getStatus())) {
			throw new IllegalArgumentException("Order is not awaiting return approval");
		}

		restoreStock(orderId);
		order.setStatus("RETURNED");
		order.setPaymentStatus("REFUNDED");
		orderRepository.save(order);

		paymentLedgerRepository.findByOrder_Id(orderId).ifPresent(ledger -> {
			ledger.setState("REFUNDED");
			paymentLedgerRepository.save(ledger);
		});

		return toFeedItem(order);
	}

	private void restoreStock(Long orderId) {
		List<OrderItem> items = orderItemRepository.findByOrder_IdOrderByIdAsc(orderId);
		for (OrderItem item : items) {
			ProductVariant variant = variantRepository.findById(item.getVariant().getId())
					.orElseThrow(() -> new IllegalArgumentException("Variant not found"));
			variant.setStock(variant.getStock() + item.getQuantity());
			variantRepository.save(variant);
		}
	}

	private void applyProduct(Product product, AdminProductRequest request) {
		product.setName(request.name().trim());
		product.setDescription(request.description());
		product.setCategory(request.category());
		product.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank() ? null : request.imageUrl().trim());
		product.setBasePrice(request.basePrice());
		product.setActive(request.active());
	}

	private AdminProductResponse toProductResponse(Product product) {
		return new AdminProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getCategory(),
				product.getImageUrl(),
				product.getBasePrice(),
				product.isActive());
	}

	private AdminPromoResponse toPromoResponse(PromoCode promo) {
		return new AdminPromoResponse(
				promo.getId(),
				promo.getCode(),
				promo.getDiscountType(),
				promo.getDiscountValue(),
				promo.getMinSubtotal(),
				promo.getMaxUses(),
				promo.getUsedCount(),
				promo.isActive(),
				promo.getValidFrom(),
				promo.getValidUntil());
	}

	private AdminOrderFeedItem toFeedItem(Order order) {
		Instant orderDate = order.getOrderDate() != null ? order.getOrderDate() : order.getCreatedAt();
		return new AdminOrderFeedItem(
				order.getId(),
				order.getUser().getEmail(),
				order.getStatus(),
				order.getTotal(),
				orderDate == null ? null : orderDate.toString(),
				orderItemRepository.countByOrder_Id(order.getId()),
				order.getActualDeliveryDate() == null ? null : order.getActualDeliveryDate().toString());
	}
}
