package com.example.ecommerse.review.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        int stars,
        String comment,
        String authorName,
        String authorEmail,
        Instant createdAt
) {
}
