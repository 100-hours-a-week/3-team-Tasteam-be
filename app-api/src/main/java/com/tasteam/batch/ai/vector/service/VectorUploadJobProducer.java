package com.tasteam.batch.ai.vector.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 벡터 업로드 배치 1사이클: 대상 레스토랑 조회 → Job N건 생성·저장 → 각 jobId 브로커에 발행.
 */
@Slf4j
@Service
public class VectorUploadJobProducer {

	private final RestaurantRepository restaurantRepository;
	private final ReviewRepository reviewRepository;
	private final AiJobRepository aiJobRepository;
	private final AiJobBroker vectorUploadJobBroker;

	public VectorUploadJobProducer(RestaurantRepository restaurantRepository,
		ReviewRepository reviewRepository,
		AiJobRepository aiJobRepository,
		@Qualifier("syncVectorUploadJobBroker")
		AiJobBroker vectorUploadJobBroker) {
		this.restaurantRepository = restaurantRepository;
		this.reviewRepository = reviewRepository;
		this.aiJobRepository = aiJobRepository;
		this.vectorUploadJobBroker = vectorUploadJobBroker;
	}

	/**
	 * 리뷰가 1건 이상 있는 비삭제 레스토랑을 대상으로 벡터 업로드 Job을 생성하고 브로커에 발행한다.
	 *
	 * @param execution 방금 저장한 VECTOR_UPLOAD_DAILY RUNNING 실행
	 */
	@Transactional
	public void createAndDispatchJobs(BatchExecution execution) {
		List<Long> restaurantIds = reviewRepository.findDistinctRestaurantIdsByDeletedAtIsNull();
		if (restaurantIds.isEmpty()) {
			log.info("No restaurants with reviews for vector upload batch, batchExecutionId={}", execution.getId());
			return;
		}
		List<Restaurant> restaurants = restaurantRepository.findByIdInAndDeletedAtIsNull(restaurantIds);
		List<AiJob> jobs = restaurants.stream()
			.map(r -> AiJob.create(
				execution,
				AiJobType.VECTOR_UPLOAD,
				r.getId(),
				r.getVectorEpoch() != null ? r.getVectorEpoch() : 0L))
			.toList();
		if (jobs.isEmpty()) {
			return;
		}
		aiJobRepository.saveAll(jobs);
		for (AiJob job : jobs) {
			vectorUploadJobBroker.publish(job.getId());
		}
		log.info("Vector upload jobs created and dispatched: batchExecutionId={}, count={}",
			execution.getId(), jobs.size());
	}
}
