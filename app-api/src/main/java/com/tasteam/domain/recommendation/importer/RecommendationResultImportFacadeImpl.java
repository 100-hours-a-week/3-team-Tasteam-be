package com.tasteam.domain.recommendation.importer;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import com.tasteam.domain.recommendation.entity.RestaurantRecommendationImportCheckpoint;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.config.RecommendationImportPollingProperties;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationImportCheckpointRepository;
import com.tasteam.global.exception.code.RecommendationErrorCode;

/**
 * 추천 결과 import 오케스트레이션 파사드.
 * 데이터 계약 기준으로 S3 탐색/대기 후 import를 수행한다.
 */
@Service
public class RecommendationResultImportFacadeImpl implements RecommendationResultImportFacade {

	private static final Logger log = LoggerFactory.getLogger(RecommendationResultImportFacadeImpl.class);

	private final S3RecommendationResultPollingService resultPollingService;
	private final RecommendationImportPollingProperties pollingProperties;
	private final RestaurantRecommendationImportCheckpointRepository checkpointRepository;
	private final RecommendationResultImportService importService;

	public RecommendationResultImportFacadeImpl(
		S3RecommendationResultPollingService resultPollingService,
		RecommendationImportPollingProperties pollingProperties,
		RestaurantRecommendationImportCheckpointRepository checkpointRepository,
		RecommendationResultImportService importService) {
		this.resultPollingService = resultPollingService;
		this.pollingProperties = pollingProperties;
		this.checkpointRepository = checkpointRepository;
		this.importService = importService;
	}

	@Override
	public RecommendationResultImportResult importResults(RecommendationResultImportFacadeCommand command) {
		Objects.requireNonNull(command, "command는 null일 수 없습니다.");

		RecommendationResultS3Target target = executeWithRetry(
			"polling",
			pollingProperties.getPollingMaxAttempts(),
			command.requestId(),
			() -> awaitResult(command));
		ensureNotImported(target);
		RecommendationResultImportResult result = executeWithRetry(
			"import",
			pollingProperties.getImportMaxAttempts(),
			command.requestId(),
			() -> importService.importResults(
				new RecommendationResultImportRequest(command.requestedModelVersion(), target.resultFileS3Uri(),
					command.requestId())));
		saveCheckpoint(target);
		return result;
	}

	private RecommendationResultS3Target awaitResult(RecommendationResultImportFacadeCommand command) {
		RecommendationResultS3Target target = resultPollingService.awaitImportTarget(
			command.resultS3Uri(),
			command.requestedModelVersion(),
			command.requestId());
		log.info("recommendation import facade result resolved. modelVersion={}, requestId={}, s3Uri={}, batchDt={}",
			target.pipelineVersion(),
			command.requestId(),
			target.resultFileS3Uri(),
			target.batchDate());
		return target;
	}

	private void ensureNotImported(RecommendationResultS3Target target) {
		if (checkpointRepository.existsByPipelineVersionAndBatchDt(target.pipelineVersion(), target.batchDate())) {
			throw RecommendationBusinessException.resultValidationFailed(
				"이미 import 완료된 추천 결과입니다. dedupKey=" + target.dedupKey());
		}
	}

	private void saveCheckpoint(RecommendationResultS3Target target) {
		try {
			checkpointRepository.save(RestaurantRecommendationImportCheckpoint.of(
				target.pipelineVersion(),
				target.batchDate(),
				Instant.now()));
		} catch (DataIntegrityViolationException ex) {
			throw RecommendationBusinessException.resultValidationFailed(
				"이미 import 완료된 추천 결과입니다. dedupKey=" + target.dedupKey());
		}
	}

	private <T> T executeWithRetry(String stage, int maxAttempts, String requestId, Callable<T> action) {
		int attempts = Math.max(1, maxAttempts);
		Throwable lastFailure = null;
		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				return action.call();
			} catch (RuntimeException ex) {
				lastFailure = ex;
				FailureClassification classification = classify(ex);
				log.warn(
					"recommendation import stage failed. stage={}, requestId={}, attempt={}/{}, failureType={}, errorCode={}, retryable={}, message={}",
					stage,
					requestId,
					attempt,
					attempts,
					classification.failureType().name(),
					classification.errorCode(),
					classification.retryable(),
					ex.getMessage());
				if (!classification.retryable() || attempt == attempts) {
					throw ex;
				}
				sleepBackoff();
			} catch (Exception ex) {
				lastFailure = ex;
				log.warn(
					"recommendation import stage failed by checked exception. stage={}, requestId={}, attempt={}/{}, message={}",
					stage,
					requestId,
					attempt,
					attempts,
					ex.getMessage());
				if (attempt == attempts) {
					throw RecommendationBusinessException.resultValidationFailed(
						"추천 import 실행 중 처리할 수 없는 예외가 발생했습니다: " + ex.getMessage());
				}
				sleepBackoff();
			}
		}
		throw RecommendationBusinessException.resultValidationFailed(
			"추천 import 재시도가 모두 실패했습니다: " + (lastFailure == null ? "unknown" : lastFailure.getMessage()));
	}

	private FailureClassification classify(RuntimeException ex) {
		if (ex instanceof RecommendationBusinessException recommendationException) {
			String code = recommendationException.getErrorCode();
			if (RecommendationErrorCode.RECOMMENDATION_RESULT_POLLING_TIMEOUT.name().equals(code)) {
				return new FailureClassification(FailureType.POLLING_TIMEOUT, true, code);
			}
			if (RecommendationErrorCode.RECOMMENDATION_RESULT_IO_ERROR.name().equals(code)) {
				return new FailureClassification(FailureType.RESULT_IO_ERROR, true, code);
			}
			if (RecommendationErrorCode.RECOMMENDATION_CSV_FORMAT_INVALID.name().equals(code)) {
				return new FailureClassification(FailureType.CSV_FORMAT_INVALID, false, code);
			}
			if (RecommendationErrorCode.RECOMMENDATION_RESULT_VALIDATION_FAILED.name().equals(code)) {
				return new FailureClassification(FailureType.RESULT_VALIDATION_FAILED, false, code);
			}
			if (RecommendationErrorCode.RECOMMENDATION_PIPELINE_VERSION_MISMATCH.name().equals(code)) {
				return new FailureClassification(FailureType.PIPELINE_VERSION_MISMATCH, false, code);
			}
			return new FailureClassification(FailureType.BUSINESS_ERROR, false, code);
		}
		if (ex instanceof TransientDataAccessException) {
			return new FailureClassification(FailureType.TRANSIENT_DATA_ACCESS, true, ex.getClass().getSimpleName());
		}
		return new FailureClassification(FailureType.UNKNOWN_RUNTIME, false, ex.getClass().getSimpleName());
	}

	private void sleepBackoff() {
		long millis = pollingProperties.getRetryBackoff() == null ? 3000L
			: pollingProperties.getRetryBackoff().toMillis();
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw RecommendationBusinessException.resultPollingTimeout("추천 import 재시도 대기 중 인터럽트가 발생했습니다.");
		}
	}

	private enum FailureType {
		POLLING_TIMEOUT,
		RESULT_IO_ERROR,
		CSV_FORMAT_INVALID,
		RESULT_VALIDATION_FAILED,
		PIPELINE_VERSION_MISMATCH,
		BUSINESS_ERROR,
		TRANSIENT_DATA_ACCESS,
		UNKNOWN_RUNTIME
	}

	private record FailureClassification(FailureType failureType, boolean retryable, String errorCode) {
	}
}
