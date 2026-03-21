package com.tasteam.batch.ai.comparison.broker;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.comparison.worker.RestaurantComparisonJobWorker;
import com.tasteam.batch.ai.service.AiJobBroker;
import com.tasteam.batch.ai.service.AiJobClaimService;

/**
 * 주간 레스토랑 비교 Job을 동기 전송: 선점 후 스레드 풀에서 실행.
 */
@Component("syncRestaurantComparisonJobBroker")
public class SyncRestaurantComparisonJobBroker implements AiJobBroker {

	private final AiJobClaimService aiJobClaimService;
	private final RestaurantComparisonJobWorker restaurantComparisonJobWorker;
	private final Executor restaurantComparisonExecutor;

	public SyncRestaurantComparisonJobBroker(AiJobClaimService aiJobClaimService,
		RestaurantComparisonJobWorker restaurantComparisonJobWorker,
		@Qualifier("restaurantComparisonExecutor")
		Executor restaurantComparisonExecutor) {
		this.aiJobClaimService = aiJobClaimService;
		this.restaurantComparisonJobWorker = restaurantComparisonJobWorker;
		this.restaurantComparisonExecutor = restaurantComparisonExecutor;
	}

	@Override
	public void publish(long jobId) {
		if (!aiJobClaimService.tryClaimToRunning(jobId)) {
			return;
		}
		restaurantComparisonExecutor.execute(() -> restaurantComparisonJobWorker.execute(jobId));
	}
}
