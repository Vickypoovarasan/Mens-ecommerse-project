package com.example.ecommerse.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ecommerse.domain.OtpVerification;
import com.example.ecommerse.domain.User;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
	Optional<OtpVerification> findTopByUserAndPurposeAndUsedFalseOrderByCreatedAtDesc(User user, String purpose);

	Optional<OtpVerification> findTopByUserAndUsedFalseOrderByCreatedAtDesc(User user);

	Optional<OtpVerification> findTopByUserOrderByCreatedAtDesc(User user);
}

