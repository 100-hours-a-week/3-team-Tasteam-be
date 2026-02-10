package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.AiRestaurantComparison;
import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.repository.AiRestaurantComparisonRepository;
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
	private final AiRestaurantComparisonRepository aiRestaurantComparisonRepository;

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

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markAnalyzingForComparison(long restaurantId) {
		AiRestaurantComparison snapshot = aiRestaurantComparisonRepository.findByRestaurantId(restaurantId)
			.orElseGet(() -> AiRestaurantComparison.createEmpty(restaurantId, AnalysisStatus.COMPLETED));
		snapshot.markAnalyzing();
		aiRestaurantComparisonRepository.save(snapshot);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markCompletedForComparison(long restaurantId) {
		aiRestaurantComparisonRepository.findByRestaurantId(restaurantId)
			.ifPresent(snapshot -> {
				snapshot.markCompleted();
				aiRestaurantComparisonRepository.save(snapshot);
			});
	}
}
