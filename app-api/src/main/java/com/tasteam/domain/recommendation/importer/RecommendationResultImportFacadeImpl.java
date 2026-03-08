package com.tasteam.domain.recommendation.importer;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 추천 결과 import 오케스트레이션 파사드.
 * 데이터 계약 기준으로 S3 탐색/대기 후 import를 수행한다.
 */
@Service
public class RecommendationResultImportFacadeImpl implements RecommendationResultImportFacade {

	private static final Logger log = LoggerFactory.getLogger(RecommendationResultImportFacadeImpl.class);

	private final S3RecommendationResultPollingService resultPollingService;
	private final RecommendationResultImportService importService;

	public RecommendationResultImportFacadeImpl(
		S3RecommendationResultPollingService resultPollingService,
		RecommendationResultImportService importService) {
		this.resultPollingService = resultPollingService;
		this.importService = importService;
	}

	@Override
	public RecommendationResultImportResult importResults(RecommendationResultImportFacadeCommand command) {
		Objects.requireNonNull(command, "command는 null일 수 없습니다.");

		String resultS3Uri = awaitResult(command);
		return importService.importResults(
			new RecommendationResultImportRequest(command.requestedModelVersion(), resultS3Uri, command.requestId()));
	}

	private String awaitResult(RecommendationResultImportFacadeCommand command) {
		String resolvedS3Uri = resultPollingService.awaitResultS3Uri(command.resultS3Uri(), command.requestId());
		log.info("recommendation import facade result resolved. modelVersion={}, requestId={}, s3Uri={}",
			command.requestedModelVersion(),
			command.requestId(),
			resolvedS3Uri);
		return resolvedS3Uri;
	}
}
