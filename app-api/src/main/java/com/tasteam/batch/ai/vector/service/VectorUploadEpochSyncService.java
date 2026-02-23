package com.tasteam.batch.ai.vector.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService.RestaurantWithReviews;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 벡터 업로드 성공 시 Restaurant·Review의 벡터 에폭 및 동기화 시각 갱신.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorUploadEpochSyncService {

	private final RestaurantRepository restaurantRepository;
	private final ReviewRepository reviewRepository;

	/**
	 * Restaurant는 baseEpoch와 일치할 때만 epoch 증가, Review는 해당 레스토랑 소속·업로드한 id만 갱신.
	 *
	 * @param job  baseEpoch 사용 (WHERE vector_epoch = baseEpoch)
	 * @param data 업로드한 레스토랑·리뷰 (리뷰 id 목록용)
	 * @param syncedAt 갱신 시각
	 * @return Restaurant가 정상 갱신되었으면 true (0건이면 다른 트랜잭션이 선점 → false)
	 */
	@Transactional
	public boolean syncEpochAfterUpload(AiJob job, RestaurantWithReviews data, Instant syncedAt) {
		Long restaurantId = job.getRestaurantId();
		long expectedEpoch = job.getBaseEpoch();

		int restaurantUpdated = restaurantRepository.incrementVectorEpochIfMatch(
			restaurantId, expectedEpoch, syncedAt);
		if (restaurantUpdated == 0) {
			log.debug("Vector epoch not updated (concurrent), restaurantId={}, expectedEpoch={}",
				restaurantId, expectedEpoch);
			return false;
		}

		List<Long> reviewIds = data.reviews().stream().map(r -> r.getId()).toList();
		if (!reviewIds.isEmpty()) {
			reviewRepository.markVectorSyncedByIdsAndRestaurant(reviewIds, restaurantId, syncedAt);
		}
		return true;
	}
}
