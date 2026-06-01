package com.example.ecommerse.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.ecommerse.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	Page<Product> findByActiveTrue(Pageable pageable);

	Page<Product> findByActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);

	@Query("""
			SELECT p FROM Product p
			WHERE p.active = true
			AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
			AND (
				:q IS NULL OR :q = ''
				OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))
				OR LOWER(p.category) LIKE LOWER(CONCAT('%', :q, '%'))
			)
			""")
	Page<Product> searchActive(
			@Param("category") String category,
			@Param("q") String q,
			Pageable pageable);

	Optional<Product> findByIdAndActiveTrue(Long id);

	List<Product> findAllByOrderByNameAsc();
}
