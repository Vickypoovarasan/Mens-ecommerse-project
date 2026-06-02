package com.example.ecommerse.review;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ecommerse.domain.OrderItem;
import com.example.ecommerse.domain.Review;
import com.example.ecommerse.repo.OrderItemRepository;
import com.example.ecommerse.repo.ReviewRepository;
import com.example.ecommerse.review.dto.ReviewRequest;
import com.example.ecommerse.review.dto.ReviewResponse;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;

    public ReviewService(ReviewRepository reviewRepository, OrderItemRepository orderItemRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listReviews(Long productId) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public double averageRating(Long productId) {
        List<ReviewResponse> reviews = listReviews(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream().mapToInt(ReviewResponse::stars).average().orElse(0.0);
    }

    @Transactional(readOnly = true)
    public int reviewCount(Long productId) {
        return reviewRepository.findByProduct_IdOrderByCreatedAtDesc(productId).size();
    }

    @Transactional(readOnly = true)
    public boolean canReview(Long productId, Long userId) {
        if (userId == null) {
            return false;
        }
        boolean hasDeliveredPurchase = orderItemRepository
                .existsByOrder_User_IdAndOrder_StatusAndProduct_Id(userId, "DELIVERED", productId);
        if (!hasDeliveredPurchase) {
            return false;
        }
        return !reviewRepository.existsByProduct_IdAndUser_Id(productId, userId);
    }

    @Transactional
    public ReviewResponse submitReview(Long userId, Long productId, ReviewRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        if (reviewRepository.existsByProduct_IdAndUser_Id(productId, userId)) {
            throw new IllegalArgumentException("You have already reviewed this product.");
        }

        OrderItem orderItem = orderItemRepository
                .findFirstByOrder_User_IdAndOrder_StatusAndProduct_Id(userId, "DELIVERED", productId)
                .orElseThrow(() -> new IllegalArgumentException("Review allowed only for delivered purchases."));

        Review review = new Review();
        review.setProduct(orderItem.getProduct());
        review.setUser(orderItem.getOrder().getUser());
        review.setOrder(orderItem.getOrder());
        review.setStars(request.stars());
        review.setComment(request.comment());

        return toResponse(reviewRepository.save(review));
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getStars(),
                review.getComment(),
                review.getUser().getUsername(),
                review.getUser().getEmail(),
                review.getCreatedAt());
    }
}
