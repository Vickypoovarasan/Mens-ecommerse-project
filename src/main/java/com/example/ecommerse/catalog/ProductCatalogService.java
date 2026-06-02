package com.example.ecommerse.catalog;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.catalog.dto.ProductDetailResponse;
import com.example.ecommerse.catalog.dto.ProductPageResponse;
import com.example.ecommerse.catalog.dto.ProductSummaryResponse;
import com.example.ecommerse.catalog.dto.VariantResponse;
import com.example.ecommerse.domain.Product;
import com.example.ecommerse.domain.ProductVariant;
import com.example.ecommerse.repo.ProductRepository;
import com.example.ecommerse.repo.ProductVariantRepository;
import com.example.ecommerse.review.ReviewService;
import com.example.ecommerse.review.dto.ReviewResponse;

@Service
public class ProductCatalogService {

	private final ProductRepository productRepository;
	private final ProductVariantRepository variantRepository;
	private final ReviewService reviewService;

	public ProductCatalogService(ProductRepository productRepository, ProductVariantRepository variantRepository,
			ReviewService reviewService) {
		this.productRepository = productRepository;
		this.variantRepository = variantRepository;
		this.reviewService = reviewService;
	}

	@Transactional(readOnly = true)
	public ProductPageResponse listProducts(String category, String query, int page, int size) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 50);
		PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());

		String cat = (category == null || category.isBlank()) ? null : category.trim();
		String q = (query == null || query.isBlank()) ? null : query.trim();

		Page<Product> result = (cat == null && q == null)
				? productRepository.findByActiveTrue(pageable)
				: productRepository.searchActive(cat, q, pageable);

		List<ProductSummaryResponse> items = result.getContent().stream()
				.map(this::toSummary)
				.toList();

		return new ProductPageResponse(
				items,
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages());
	}

	@Transactional(readOnly = true)
	public ProductDetailResponse getProduct(Long id, Long currentUserId) {
		Product product = productRepository.findByIdAndActiveTrue(id)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));

		List<VariantResponse> variants = variantRepository.findByProductIdAndActiveTrueOrderBySizeAsc(id).stream()
				.map(this::toVariant)
				.toList();

		List<ReviewResponse> reviews = reviewService.listReviews(id);
		double averageRating = reviewService.averageRating(id);
		int reviewCount = reviewService.reviewCount(id);
		boolean canReview = reviewService.canReview(id, currentUserId);

		return new ProductDetailResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getCategory(),
				product.getBasePrice(),
				imageKeyFor(product),
				product.getImageUrl(),
				averageRating,
				reviewCount,
				reviews,
				canReview,
				variants);
	}

	private ProductSummaryResponse toSummary(Product product) {
		return new ProductSummaryResponse(
				product.getId(),
				product.getName(),
				product.getCategory(),
				product.getBasePrice(),
				imageKeyFor(product),
				product.getImageUrl());
	}

	private VariantResponse toVariant(ProductVariant variant) {
		return new VariantResponse(
				variant.getId(),
				variant.getSize(),
				variant.getColor(),
				variant.getSku(),
				variant.getPrice(),
				variant.getStock(),
				variant.getStock() > 0);
	}

	private String imageKeyFor(Product product) {
		String category = product.getCategory() == null ? "default" : product.getCategory().toLowerCase();
		return switch (category) {
			case "suits" -> "suit";
			case "shirts" -> "shirt";
			default -> "default";
		};
	}
}
