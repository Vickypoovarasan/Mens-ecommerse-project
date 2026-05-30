package com.example.ecommerse.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.auth.dto.LoginResponse;
import com.example.ecommerse.domain.RefreshToken;
import com.example.ecommerse.domain.User;
import com.example.ecommerse.repo.RefreshTokenRepository;
import com.example.ecommerse.repo.UserRepository;

@Service
public class AuthService {

	private static final Duration REFRESH_TTL = Duration.ofDays(30);

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, JwtService jwtService) {
		this.userRepository = userRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.jwtService = jwtService;
	}

	@Transactional
	public LoginResult login(String email, String password) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

		if (!user.isVerified()) {
			throw new IllegalArgumentException("Please verify OTP before login");
		}

		if (!passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new IllegalArgumentException("Invalid credentials");
		}

		String accessToken = jwtService.createAccessToken(user);
		String refreshToken = generateRefreshToken();

		RefreshToken rt = new RefreshToken();
		rt.setUser(user);
		rt.setTokenHash(sha256(refreshToken));
		rt.setExpiresAt(Instant.now().plus(REFRESH_TTL));
		rt.setRevoked(false);
		refreshTokenRepository.save(rt);

		return new LoginResult(new LoginResponse(accessToken, jwtService.accessTtl().toSeconds()), refreshToken);
	}

	@Transactional
	public LoginResult refresh(String refreshToken) {
		byte[] hash = sha256(refreshToken);
		RefreshToken rt = refreshTokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

		if (rt.isRevoked()) {
			throw new IllegalArgumentException("Invalid refresh token");
		}
		if (Instant.now().isAfter(rt.getExpiresAt())) {
			throw new IllegalArgumentException("Refresh token expired");
		}

		User user = rt.getUser();
		String accessToken = jwtService.createAccessToken(user);

		// rotate refresh token
		rt.setRevoked(true);
		refreshTokenRepository.save(rt);

		String newRefresh = generateRefreshToken();
		RefreshToken next = new RefreshToken();
		next.setUser(user);
		next.setTokenHash(sha256(newRefresh));
		next.setExpiresAt(Instant.now().plus(REFRESH_TTL));
		next.setRevoked(false);
		refreshTokenRepository.save(next);

		return new LoginResult(new LoginResponse(accessToken, jwtService.accessTtl().toSeconds()), newRefresh);
	}

	@Transactional
	public void logout(String refreshToken) {
		byte[] hash = sha256(refreshToken);
		refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
			rt.setRevoked(true);
			refreshTokenRepository.save(rt);
		});
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private byte[] sha256(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(token.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			throw new IllegalStateException("Hash failure", e);
		}
	}

	public record LoginResult(LoginResponse response, String refreshToken) {
	}
}

