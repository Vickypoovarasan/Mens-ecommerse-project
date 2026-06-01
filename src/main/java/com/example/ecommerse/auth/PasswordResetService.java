package com.example.ecommerse.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.auth.dto.ResetPasswordRequest;
import com.example.ecommerse.domain.User;
import com.example.ecommerse.repo.UserRepository;

@Service
public class PasswordResetService {

	private static final String GENERIC_FORGOT_MSG =
			"If an account exists for this email, a reset code has been sent.";

	private final UserRepository userRepository;
	private final OtpService otpService;
	private final PasswordEncoder passwordEncoder;

	public PasswordResetService(UserRepository userRepository, OtpService otpService) {
		this.userRepository = userRepository;
		this.otpService = otpService;
		this.passwordEncoder = new BCryptPasswordEncoder();
	}

	@Transactional
	public String requestReset(String email) {
		userRepository.findByEmail(email.trim())
				.filter(User::isVerified)
				.ifPresent(user -> otpService.issueOtp(user, OtpPurpose.PASSWORD_RESET, "password reset"));
		return GENERIC_FORGOT_MSG;
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		User user = userRepository.findByEmail(request.email().trim())
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (!user.isVerified()) {
			throw new IllegalArgumentException("Account is not verified");
		}

		otpService.consumeOtp(user, request.otp(), OtpPurpose.PASSWORD_RESET);
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}
}
