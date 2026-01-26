package com.tasteam.domain.review.dto;

public record RestaurantReviewSearchCondition(long restaurantId, ReviewCursor cursor, int size) {
}
