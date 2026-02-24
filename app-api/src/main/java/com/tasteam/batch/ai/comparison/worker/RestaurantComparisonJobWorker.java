package com.tasteam.batch.ai.comparison.worker;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.AiJobType;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.restaurant.service.analysis.RestaurantReviewAnalysisSnapshotService;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiStrengthsRequest;
import com.tasteam.infra.ai.dto.AiStrengthsResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선점된 주간 레스토랑 비교 Job 1건 실행: AI 비교 분석 호출 후 DB 저장.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantComparisonJobWorker {

	private static final int TOP_K = 10;

	private final AiJobRepository aiJobRepository;
	private final AiClient aiClient;
	private final RestaurantReviewAnalysisSnapshotService snapshotService;
	private final TransactionTemplate transactionTemplate;

	/**
	 * jobId로 Job 조회 후 실행. 없거나 RUNNING이 아니면 스킵.
	 */
	public void execute(long jobId) {
		Optional<AiJob> opt = aiJobRepository.findById(jobId);
		if (opt.isEmpty()) {
			log.warn("Restaurant comparison job not found, skipping: jobId={}", jobId);
			return;
		}
		AiJob job = opt.get();
		if (job.getStatus() != AiJobStatus.RUNNING) {
			log.debug("Restaurant comparison job not RUNNING, skipping: jobId={}, status={}", jobId, job.getStatus());
			return;
		}
		if (job.getJobType() != AiJobType.RESTAURANT_COMPARISON) {
			log.warn("Unexpected job type for comparison worker: jobId={}, type={}", jobId, job.getJobType());
			markFailedInTx(job);
			return;
		}

		long restaurantId = job.getRestaurantId();
		try {
			AiStrengthsResponse response = aiClient.extractStrengths(new AiStrengthsRequest(restaurantId, TOP_K));
			Map<String, BigDecimal> categoryLift = toBigDecimalMap(response.categoryLift());
			List<String> comparisonDisplay = Optional.ofNullable(response.strengthDisplay()).orElse(List.of());
			Instant analyzedAt = Instant.now();

			transactionTemplate.executeWithoutResult(__ -> {
				snapshotService.saveComparison(
					restaurantId,
					categoryLift,
					comparisonDisplay,
					response.totalCandidates(),
					response.validatedCount(),
					analyzedAt);
				job.markCompleted();
				aiJobRepository.save(job);
			});
			log.debug("Restaurant comparison job completed: jobId={}, restaurantId={}", jobId, restaurantId);
		} catch (Exception e) {
			log.error("Restaurant comparison AI call failed: jobId={}, restaurantId={}", jobId, restaurantId, e);
			markFailedInTx(job);
		}
	}

	private static Map<String, BigDecimal> toBigDecimalMap(Map<String, Double> source) {
		if (source == null || source.isEmpty()) {
			return Map.of();
		}
		return source.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> BigDecimal.valueOf(e.getValue())));
	}

	private void markFailedInTx(AiJob job) {
		transactionTemplate.executeWithoutResult(__ -> {
			job.markFailed();
			aiJobRepository.save(job);
		});
	}
}
