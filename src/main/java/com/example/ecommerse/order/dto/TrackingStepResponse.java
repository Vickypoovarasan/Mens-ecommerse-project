package com.example.ecommerse.order.dto;

public record TrackingStepResponse(
		String code,
		String label,
		boolean completed,
		boolean active) {
}
