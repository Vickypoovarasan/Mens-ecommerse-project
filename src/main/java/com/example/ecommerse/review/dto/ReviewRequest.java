package com.example.ecommerse.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @Min(1)
        @Max(5)
        int stars,
        @Size(max = 1000)
        String comment
) {
}
