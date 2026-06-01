package com.example.ecommerse.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.example.ecommerse.domain.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Service
public class JwtService {

	private static final Duration ACCESS_TTL = Duration.ofMinutes(15);

	private final SecretKey key;

	public JwtService() {
		// Dev-only secret. Move to env var before production.
		byte[] secret = "local-dev-jwt-secret-local-dev-jwt-secret".getBytes(StandardCharsets.UTF_8);
		this.key = Keys.hmacShaKeyFor(secret);
	}

	public String createAccessToken(User user) {
		Instant now = Instant.now();
		Instant exp = now.plus(ACCESS_TTL);

		return Jwts.builder()
				.subject(user.getId().toString())
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.claim("email", user.getEmail())
				.claim("role", user.getRole())
				.signWith(key)
				.compact();
	}

	public Duration accessTtl() {
		return ACCESS_TTL;
	}

	public SecretKey key() {
		return key;
	}
}

