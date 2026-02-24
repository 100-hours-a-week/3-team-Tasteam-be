package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.repository.AiRestaurantReviewAnalysisRepository;
import com.tasteam.domain.restaurant.type.AnalysisStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantReviewAnalysisStateService {

	// NOTE:
	// - 이 서비스는 AI 분석 "상태 스냅샷" 저장/전이만 담당한다.
	// - 비즈니스 엔티티(Review, Restaurant 등) 변경 로직은 다루지 않는다.
	// - REQUIRES_NEW는 분석 호출 실패와 스냅샷 상태 전이를 분리하기 위한 용도로만 사용한다.
	// TODO: 상태 전이 API를 transition/updateStatus 형태로 단순화 검토

	private final AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markAnalyzingForSummary(long restaurantId) {
		AiRestaurantReviewAnalysis snapshot = aiRestaurantReviewAnalysisRepository.findByRestaurantId(restaurantId)
			.orElseGet(() -> AiRestaurantReviewAnalysis.createEmpty(restaurantId, AnalysisStatus.COMPLETED));
		snapshot.markAnalyzing();
		aiRestaurantReviewAnalysisRepository.save(snapshot);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markCompletedForSummary(long restaurantId) {
		aiRestaurantReviewAnalysisRepository.findByRestaurantId(restaurantId)
			.ifPresent(snapshot -> {
				snapshot.markCompleted();
				aiRestaurantReviewAnalysisRepository.save(snapshot);
			});
	}

	/**
	 * 비교 분석은 restaurant_comparison 테이블에만 저장. 상태 전이는 사용하지 않음 (no-op).
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markAnalyzingForComparison(long restaurantId) {
		// no-op: 비교 결과는 RestaurantComparison에만 저장
	}

	/**
	 * 비교 분석은 restaurant_comparison 테이블에만 저장. 상태 전이는 사용하지 않음 (no-op).
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markCompletedForComparison(long restaurantId) {
		// no-op: 비교 결과는 RestaurantComparison에만 저장
	}
}
