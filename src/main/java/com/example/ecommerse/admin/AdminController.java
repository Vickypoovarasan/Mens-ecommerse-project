package com.example.ecommerse.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ecommerse.admin.dto.AdminOrderFeedItem;
import com.example.ecommerse.admin.dto.AdminProductRequest;
import com.example.ecommerse.admin.dto.AdminProductResponse;
import com.example.ecommerse.admin.dto.AdminVariantRequest;
import com.example.ecommerse.admin.dto.AdminVariantResponse;
import com.example.ecommerse.admin.dto.AdminVariantStockRequest;
import com.example.ecommerse.admin.dto.UpdateOrderStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/products")
	public ResponseEntity<List<AdminProductResponse>> listProducts() {
		return ResponseEntity.ok(adminService.listProducts());
	}

	@PostMapping("/products")
	public ResponseEntity<AdminProductResponse> createProduct(@Valid @RequestBody AdminProductRequest request) {
		return ResponseEntity.ok(adminService.createProduct(request));
	}

	@PutMapping("/products/{id}")
	public ResponseEntity<AdminProductResponse> updateProduct(
			@PathVariable Long id,
			@Valid @RequestBody AdminProductRequest request) {
		return ResponseEntity.ok(adminService.updateProduct(id, request));
	}

	@GetMapping("/products/{productId}/variants")
	public ResponseEntity<List<AdminVariantResponse>> listVariants(
			@PathVariable Long productId) {
		return ResponseEntity.ok(adminService.listVariants(productId));
	}

	@PostMapping("/products/{productId}/variants")
	public ResponseEntity<AdminVariantResponse> createVariant(
			@PathVariable Long productId,
			@Valid @RequestBody AdminVariantRequest request) {
		return ResponseEntity.ok(adminService.createVariant(productId, request));
	}

	@PutMapping("/variants/{id}")
	public ResponseEntity<AdminVariantResponse> updateVariant(
			@PathVariable Long id,
			@Valid @RequestBody AdminVariantRequest request) {
		return ResponseEntity.ok(adminService.updateVariant(id, request));
	}

	@PatchMapping("/variants/{id}/stock")
	public ResponseEntity<Void> updateVariantStock(
			@PathVariable Long id,
			@Valid @RequestBody AdminVariantStockRequest request) {
		adminService.updateVariantStock(id, request.stock());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/orders/dispatch")
	public ResponseEntity<List<AdminOrderFeedItem>> dispatchFeed() {
		return ResponseEntity.ok(adminService.dispatchFeed());
	}

	@GetMapping("/orders/returns")
	public ResponseEntity<List<AdminOrderFeedItem>> returnQueue() {
		return ResponseEntity.ok(adminService.returnQueue());
	}

	@PatchMapping("/orders/{id}/status")
	public ResponseEntity<AdminOrderFeedItem> updateStatus(
			@PathVariable Long id,
			@Valid @RequestBody UpdateOrderStatusRequest request) {
		return ResponseEntity.ok(adminService.updateOrderStatus(id, request));
	}

	@PostMapping("/orders/{id}/returns/approve")
	public ResponseEntity<AdminOrderFeedItem> approveReturn(@PathVariable Long id) {
		return ResponseEntity.ok(adminService.approveReturn(id));
	}
}
