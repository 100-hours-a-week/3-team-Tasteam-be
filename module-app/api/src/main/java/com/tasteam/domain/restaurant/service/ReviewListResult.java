package com.tasteam.domain.restaurant.service;

import java.util.List;

import com.tasteam.domain.review.dto.ReviewCursor;
import com.tasteam.domain.review.dto.ReviewSummaryQueryDto;

public record ReviewListResult(List<ReviewSummaryQueryDto> items, ReviewCursor nextCursor, boolean hasNext, int size) {
}
