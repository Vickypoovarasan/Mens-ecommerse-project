package com.example.ecommerse.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerse.domain.IdempotencyKey;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

	Optional<IdempotencyKey> findByUser_IdAndKey(Long userId, String key);
}

