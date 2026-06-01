package com.example.ecommerse.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.ecommerse.domain.PromoCode;

public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

	Optional<PromoCode> findByCodeAndActiveTrue(String code);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE PromoCode p SET p.usedCount = p.usedCount + 1
			WHERE p.id = :id AND (p.maxUses IS NULL OR p.usedCount < p.maxUses)
			""")
	int incrementUsedCount(@Param("id") Long id);
}
