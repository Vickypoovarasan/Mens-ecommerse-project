package com.example.ecommerse.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.example.ecommerse.domain.ProductVariant;

import jakarta.persistence.LockModeType;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

	List<ProductVariant> findByProductIdAndActiveTrueOrderBySizeAsc(Long productId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ProductVariant> findByIdAndActiveTrue(Long id);

	List<ProductVariant> findByProductIdOrderBySizeAsc(Long productId);

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, Long id);

	boolean existsByProduct_IdAndSizeAndColor(Long productId, String size, String color);

	boolean existsByProduct_IdAndSizeAndColorAndIdNot(Long productId, String size, String color, Long id);
}
