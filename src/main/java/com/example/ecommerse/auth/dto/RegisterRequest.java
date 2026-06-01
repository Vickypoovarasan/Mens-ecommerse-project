package com.example.ecommerse.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(max = 50) String username,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) String password,
		@Size(max = 20) String phone,
		@Size(max = 500) String address,
		@Past @NotNull LocalDate dob
) {
}

