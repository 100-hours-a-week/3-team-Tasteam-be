package com.tasteam.domain.recommendation.importer;

import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.tasteam.domain.recommendation.entity.RestaurantRecommendationImportCheckpoint;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationImportCheckpointRepository;

/**
 * 추천 결과 import 오케스트레이션 파사드.
 * 데이터 계약 기준으로 S3 탐색/대기 후 import를 수행한다.
 */
@Service
public class RecommendationResultImportFacadeImpl implements RecommendationResultImportFacade {

	private static final Logger log = LoggerFactory.getLogger(RecommendationResultImportFacadeImpl.class);

	private final S3RecommendationResultPollingService resultPollingService;
	private final RestaurantRecommendationImportCheckpointRepository checkpointRepository;
	private final RecommendationResultImportService importService;

	public RecommendationResultImportFacadeImpl(
		S3RecommendationResultPollingService resultPollingService,
		RestaurantRecommendationImportCheckpointRepository checkpointRepository,
		RecommendationResultImportService importService) {
		this.resultPollingService = resultPollingService;
		this.checkpointRepository = checkpointRepository;
		this.importService = importService;
	}

	@Override
	public RecommendationResultImportResult importResults(RecommendationResultImportFacadeCommand command) {
		Objects.requireNonNull(command, "command는 null일 수 없습니다.");

		RecommendationResultS3Target target = awaitResult(command);
		ensureNotImported(target);
		RecommendationResultImportResult result = importService.importResults(
			new RecommendationResultImportRequest(command.requestedModelVersion(), target.resultFileS3Uri(),
				command.requestId()));
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
}
