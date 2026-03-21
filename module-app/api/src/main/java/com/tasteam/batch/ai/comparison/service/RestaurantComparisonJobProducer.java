package com.tasteam.batch.ai.comparison.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tasteam.batch.ai.service.AiJobBroker;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobType;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.repository.ReviewRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 주간 레스토랑 비교 배치: 리뷰가 있는 전체 레스토랑에 대해 비교 분석 Job N건 생성·브로커 발행.
 * 벡터 업로드 없이 baseEpoch=0 으로 Job 생성.
 */
@Slf4j
@Service
public class RestaurantComparisonJobProducer {

	private static final long BASE_EPOCH = 0L;

	private final ReviewRepository reviewRepository;
	private final RestaurantRepository restaurantRepository;
	private final AiJobRepository aiJobRepository;
	private final AiJobBroker restaurantComparisonJobBroker;

	public RestaurantComparisonJobProducer(ReviewRepository reviewRepository,
		RestaurantRepository restaurantRepository,
		AiJobRepository aiJobRepository,
		@Qualifier("syncRestaurantComparisonJobBroker")
		AiJobBroker restaurantComparisonJobBroker) {
		this.reviewRepository = reviewRepository;
		this.restaurantRepository = restaurantRepository;
		this.aiJobRepository = aiJobRepository;
		this.restaurantComparisonJobBroker = restaurantComparisonJobBroker;
	}

	/**
	 * 리뷰가 1건 이상 있는 비삭제 레스토랑을 대상으로 비교 분석 Job을 생성하고 브로커에 발행한다.
	 *
	 * @param execution 방금 저장한 RESTAURANT_COMPARISON_WEEKLY RUNNING 실행
	 */
	@Transactional
	public void createAndDispatchJobs(BatchExecution execution) {
		List<Long> restaurantIds = reviewRepository.findDistinctRestaurantIdsByDeletedAtIsNull();
		if (restaurantIds.isEmpty()) {
			log.info("No restaurants with reviews for comparison batch, batchExecutionId={}", execution.getId());
			return;
		}
		List<Restaurant> restaurants = restaurantRepository.findByIdInAndDeletedAtIsNull(restaurantIds);
		List<AiJob> jobs = restaurants.stream()
			.map(r -> AiJob.create(execution, AiJobType.RESTAURANT_COMPARISON, r.getId(), BASE_EPOCH))
			.toList();
		if (jobs.isEmpty()) {
			return;
		}
		aiJobRepository.saveAll(jobs);
		List<Long> jobIds = jobs.stream().map(AiJob::getId).toList();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				for (Long jobId : jobIds) {
					restaurantComparisonJobBroker.publish(jobId);
				}
				log.info("Restaurant comparison jobs created and dispatched: batchExecutionId={}, count={}",
					execution.getId(), jobIds.size());
			}
		});
	}
}
