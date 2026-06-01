package com.example.ecommerse.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.auth.dto.RegisterRequest;
import com.example.ecommerse.domain.OtpVerification;
import com.example.ecommerse.domain.User;
import com.example.ecommerse.repo.OtpVerificationRepository;
import com.example.ecommerse.repo.UserRepository;

@Service
public class OtpService {

	private static final Duration OTP_TTL = Duration.ofMinutes(5);
	private static final int MAX_ATTEMPTS = 5;

	private final UserRepository userRepository;
	private final OtpVerificationRepository otpRepository;
	private final OtpEmailService otpEmailService;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom secureRandom = new SecureRandom();
	private final byte[] otpHmacKey;

	public OtpService(
			UserRepository userRepository,
			OtpVerificationRepository otpRepository,
			OtpEmailService otpEmailService) {
		this.userRepository = userRepository;
		this.otpRepository = otpRepository;
		this.otpEmailService = otpEmailService;
		this.passwordEncoder = new BCryptPasswordEncoder();
		this.otpHmacKey = "local-dev-otp-secret-change-me".getBytes(StandardCharsets.UTF_8);
	}

	@Transactional
	public void register(RegisterRequest req) {
		userRepository.findByEmail(req.email()).ifPresent(u -> {
			throw new IllegalArgumentException("Email already registered");
		});
		userRepository.findByUsername(req.username()).ifPresent(u -> {
			throw new IllegalArgumentException("Username already registered");
		});

		User user = new User();
		user.setUsername(req.username());
		user.setEmail(req.email());
		user.setPasswordHash(passwordEncoder.encode(req.password()));
		user.setPhone(req.phone());
		user.setAddress(req.address());
		user.setDob(req.dob());
		user.setRole("CUSTOMER");
		user.setVerified(false);
		user = userRepository.save(user);

		issueOtp(user, OtpPurpose.REGISTRATION, "registration");
	}

	@Transactional
	public void verifyOtp(String email, String otp) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		consumeOtp(user, otp, OtpPurpose.REGISTRATION);
		user.markVerified();
		userRepository.save(user);
	}

	@Transactional
	public void resendOtp(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (user.isVerified()) {
			throw new IllegalArgumentException("User already verified");
		}

		issueOtp(user, OtpPurpose.REGISTRATION, "resend");
	}

	@Transactional
	public void issueOtp(User user, String purpose, String emailLabel) {
		String otp = generateOtp();
		byte[] otpHash = hmacSha256(otp);

		OtpVerification v = new OtpVerification();
		v.setUser(user);
		v.setPurpose(purpose);
		v.setOtpHash(otpHash);
		v.setExpiresAt(Instant.now().plus(OTP_TTL));
		v.setAttempts(0);
		v.setUsed(false);
		otpRepository.save(v);

		otpEmailService.sendOtp(user.getEmail(), otp, emailLabel);
	}

	@Transactional
	public void consumeOtp(User user, String otp, String purpose) {
		OtpVerification v = otpRepository.findTopByUserAndPurposeAndUsedFalseOrderByCreatedAtDesc(user, purpose)
				.orElseThrow(() -> new IllegalArgumentException("No active OTP. Please request a new code."));

		if (v.isUsed()) {
			throw new IllegalArgumentException("OTP already used");
		}
		if (Instant.now().isAfter(v.getExpiresAt())) {
			throw new IllegalArgumentException("OTP expired");
		}
		if (v.getAttempts() >= MAX_ATTEMPTS) {
			throw new IllegalStateException("Too many attempts. Please request a new OTP.");
		}

		byte[] providedHash = hmacSha256(otp);
		boolean matches = MessageDigest.isEqual(v.getOtpHash(), providedHash);

		if (!matches) {
			v.incrementAttempts();
			otpRepository.save(v);
			throw new IllegalArgumentException("Invalid OTP");
		}

		v.markUsed();
		otpRepository.save(v);
	}

	private String generateOtp() {
		int value = secureRandom.nextInt(1_000_000);
		return String.format("%06d", value);
	}

	private byte[] hmacSha256(String otp) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(otpHmacKey, "HmacSHA256"));
			return mac.doFinal(otp.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to hash OTP", e);
		}
	}
}
