package com.example.ecommerse.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateOrderStatusRequest(
		@NotBlank @Size(max = 30) String status) {
}
