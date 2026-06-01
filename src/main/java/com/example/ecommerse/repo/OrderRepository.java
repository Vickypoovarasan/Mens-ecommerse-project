package com.example.ecommerse.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerse.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUser_IdOrderByCreatedAtDesc(Long userId);

	Optional<Order> findByIdAndUser_Id(Long id, Long userId);

	List<Order> findByStatusInOrderByCreatedAtDesc(Collection<String> statuses);

	List<Order> findByStatusOrderByCreatedAtDesc(String status);
}

