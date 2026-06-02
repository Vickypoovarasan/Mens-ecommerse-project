package com.example.ecommerse.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerse.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    boolean existsByProduct_IdAndUser_Id(Long productId, Long userId);

    boolean existsByOrder_User_IdAndOrder_StatusAndProduct_Id(Long userId, String status, Long productId);

    Optional<Review> findByProduct_IdAndUser_Id(Long productId, Long userId);
}
