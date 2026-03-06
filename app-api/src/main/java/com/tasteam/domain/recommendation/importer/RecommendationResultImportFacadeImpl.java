package com.tasteam.domain.recommendation.importer;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 추천 결과 import 오케스트레이션 파사드.
 * 현재 단계에서는 흐름을 고정하고(요청 -> 대기 -> import), 실제 학습 요청/대기는 후속 티켓에서 구현한다.
 */
@Service
public class RecommendationResultImportFacadeImpl implements RecommendationResultImportFacade {

	private static final Logger log = LoggerFactory.getLogger(RecommendationResultImportFacadeImpl.class);

	private final AiRecommendationJobClient aiRecommendationJobClient;
	private final RecommendationResultImportService importService;

	public RecommendationResultImportFacadeImpl(
		AiRecommendationJobClient aiRecommendationJobClient,
		RecommendationResultImportService importService) {
		this.aiRecommendationJobClient = aiRecommendationJobClient;
		this.importService = importService;
	}

	@Override
	public RecommendationResultImportResult importResults(RecommendationResultImportFacadeCommand command) {
		Objects.requireNonNull(command, "command는 null일 수 없습니다.");

		requestRecommendation(command);
		String resultS3Uri = awaitResult(command);
		return importService.importResults(
			new RecommendationResultImportRequest(command.requestedModelVersion(), resultS3Uri, command.requestId()));
	}

	private void requestRecommendation(RecommendationResultImportFacadeCommand command) {
		AiRecommendationResponse response = aiRecommendationJobClient.requestRecommendation(
			new AiRecommendationRequest(command.requestedModelVersion(), command.requestId()));
		log.info("recommendation import facade request sent. modelVersion={}, requestId={}, jobId={}",
			command.requestedModelVersion(),
			command.requestId(),
			response.jobId());
	}

	private String awaitResult(RecommendationResultImportFacadeCommand command) {
		log.info("recommendation import facade waiting skipped (skeleton). modelVersion={}, requestId={}, s3Uri={}",
			command.requestedModelVersion(),
			command.requestId(),
			command.resultS3Uri());
		return command.resultS3Uri();
	}
}
