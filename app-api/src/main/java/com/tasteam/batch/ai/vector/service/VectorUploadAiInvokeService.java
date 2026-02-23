package com.tasteam.batch.ai.vector.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService.RestaurantWithReviews;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiVectorUploadRequest;
import com.tasteam.infra.ai.dto.AiVectorUploadResponse;

import lombok.RequiredArgsConstructor;

/**
 * AI 벡터 업로드 호출.
 */
@Service
@RequiredArgsConstructor
public class VectorUploadAiInvokeService {

	private final AiClient aiClient;

	/**
	 * 레스토랑·리뷰 데이터로 벡터 업로드 API 호출.
	 * 성공 시 {@link VectorUploadInvokeResult.Success}, 예외 시 {@link VectorUploadInvokeResult.Failure} 반환.
	 */
	public VectorUploadInvokeResult invoke(RestaurantWithReviews data) {
		AiVectorUploadRequest request = toRequest(data);
		try {
			AiVectorUploadResponse response = aiClient.uploadVectorData(request);
			return new VectorUploadInvokeResult.Success(response);
		} catch (Exception e) {
			return new VectorUploadInvokeResult.Failure(e);
		}
	}

	private AiVectorUploadRequest toRequest(RestaurantWithReviews data) {
		long restaurantId = data.restaurant().getId();
		List<AiVectorUploadRequest.ReviewPayload> reviews = data.reviews().stream()
			.map(r -> new AiVectorUploadRequest.ReviewPayload(
				r.getId(),
				restaurantId,
				r.getContent() != null ? r.getContent() : "",
				r.getCreatedAt() != null ? r.getCreatedAt() : Instant.now()))
			.toList();
		List<AiVectorUploadRequest.RestaurantPayload> restaurants = List.of(
			new AiVectorUploadRequest.RestaurantPayload(
				data.restaurant().getId(),
				data.restaurant().getName()));
		return new AiVectorUploadRequest(reviews, restaurants);
	}
}
