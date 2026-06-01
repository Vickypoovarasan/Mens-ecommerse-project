package com.example.ecommerse.auth.dto;

public record LoginResponse(
		String accessToken,
		long expiresInSeconds
) {
}

