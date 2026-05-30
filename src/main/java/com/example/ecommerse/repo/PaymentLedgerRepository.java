package com.example.ecommerse.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerse.domain.PaymentLedger;

public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, Long> {

	Optional<PaymentLedger> findByOrder_Id(Long orderId);

	Optional<PaymentLedger> findByProviderRef(String providerRef);
}

