package com.example.ecommerse.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerse.domain.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);

	int countByOrder_Id(Long orderId);

	boolean existsByOrder_User_IdAndOrder_StatusAndProduct_Id(Long userId, String status, Long productId);

	Optional<OrderItem> findFirstByOrder_User_IdAndOrder_StatusAndProduct_Id(Long userId, String status, Long productId);
}

