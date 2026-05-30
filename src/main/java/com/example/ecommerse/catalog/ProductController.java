package com.example.ecommerse.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.catalog.dto.ProductDetailResponse;
import com.example.ecommerse.catalog.dto.ProductPageResponse;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductCatalogService catalogService;

	public ProductController(ProductCatalogService catalogService) {
		this.catalogService = catalogService;
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
	public ResponseEntity<ProductDetailResponse> get(@PathVariable Long id) {
		return ResponseEntity.ok(catalogService.getProduct(id));
	}
}
