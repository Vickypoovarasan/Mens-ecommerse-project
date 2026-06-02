package com.example.ecommerse.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.catalog.dto.ProductDetailResponse;
import com.example.ecommerse.catalog.dto.ProductPageResponse;
import com.example.ecommerse.review.ReviewService;
import com.example.ecommerse.review.dto.ReviewRequest;
import com.example.ecommerse.review.dto.ReviewResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private static final Logger log = LoggerFactory.getLogger(ProductController.class);

	private final ProductCatalogService catalogService;
	private final ReviewService reviewService;

	public ProductController(ProductCatalogService catalogService, ReviewService reviewService) {
		this.catalogService = catalogService;
		this.reviewService = reviewService;
	}

	@GetMapping
	public ResponseEntity<ProductPageResponse> list(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {
		return ResponseEntity.ok(catalogService.listProducts(category, q, page, size));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductDetailResponse> get(@PathVariable Long id, Authentication authentication) {
		Long currentUserId = authentication != null ? (Long) authentication.getPrincipal() : null;
		return ResponseEntity.ok(catalogService.getProduct(id, currentUserId));
	}

	@PostMapping("/{id}/reviews")
	public ResponseEntity<ReviewResponse> submitReview(
			@PathVariable Long id,
			@Valid @RequestBody ReviewRequest request,
			Authentication authentication) {
		Long userId = (Long) authentication.getPrincipal();
		log.info("Review submit called for product {} by user {}: stars={} comment={}", id, userId, request.stars(), request.comment());
		return ResponseEntity.ok(reviewService.submitReview(userId, id, request));
	}
}
