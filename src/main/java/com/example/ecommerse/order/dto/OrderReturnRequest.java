package com.example.ecommerse.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderReturnRequest(
        @NotBlank(message = "Return reason is required")
        @Size(max = 500, message = "Return reason must be 500 characters or less")
        String reason) {
}
