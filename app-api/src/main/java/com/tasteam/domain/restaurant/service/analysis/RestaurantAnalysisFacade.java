package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.stereotype.Service;

import com.tasteam.batch.ai.review.runner.ReviewAnalysisRunner;
import com.tasteam.batch.ai.vector.runner.VectorUploadRunResult;
import com.tasteam.batch.ai.vector.runner.VectorUploadRunner;
import com.tasteam.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 리뷰 생성 시 최초 분석: 락·정책 통과 시 벡터 업로드 Runner → 성공 시 감정·요약·비교 분석 Runner 직접 호출.
 * BatchExecution, Job, 브로커 사용 없음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantAnalysisFacade {

	private final AnalysisLock analysisLock;
	private final ComparisonAnalysisTriggerPolicy comparisonTriggerPolicy;
	private final ReviewRepository reviewRepository;
	private final VectorUploadRunner vectorUploadRunner;
	private final ReviewAnalysisRunner reviewAnalysisRunner;

	/**
	 * 리뷰 생성 시 호출. 최초 분석 정책을 만족하면 벡터 업로드 후 감정·요약·비교 분석을 실행해
	 * restaurant_review_sentiment, restaurant_review_summary, restaurant_comparison을 채운다.
	 */
	public void onReviewCreated(long restaurantId) {
		if (!analysisLock.tryLock(restaurantId)) {
			log.debug("AI analysis skipped due to lock contention. restaurantId={}", restaurantId);
			return;
		}

		try {
			int reviewCount = Math.toIntExact(reviewRepository.countByRestaurantIdAndDeletedAtIsNull(restaurantId));
			log.debug("Initial analysis trigger check on review-created. restaurantId={}, reviewCount={}", restaurantId,
				reviewCount);

			if (!comparisonTriggerPolicy.shouldRun(reviewCount)) {
				log.debug("Initial analysis skipped by policy. restaurantId={}, reviewCount={}",
					restaurantId, reviewCount);
				return;
			}

			log.info("Initial analysis triggered on review-created. restaurantId={}, reviewCount={}",
				restaurantId, reviewCount);

			VectorUploadRunResult uploadResult = vectorUploadRunner.runForEvent(restaurantId);

			switch (uploadResult) {
				case VectorUploadRunResult.Success s -> {
					long vectorEpoch = s.newVectorEpoch();
					reviewAnalysisRunner.runSentimentAnalysis(restaurantId, vectorEpoch);
					reviewAnalysisRunner.runSummaryAnalysis(restaurantId, vectorEpoch);
					reviewAnalysisRunner.runComparisonAnalysis(restaurantId);
				}
				default ->
					log.warn("Initial analysis skipped: vector upload did not succeed. restaurantId={}, result={}",
						restaurantId, uploadResult.getClass().getSimpleName());
			}
		} catch (Exception e) {
			log.warn("AI review-created analysis failed. restaurantId={}", restaurantId, e);
		} finally {
			analysisLock.unlock(restaurantId);
		}
	}
}
