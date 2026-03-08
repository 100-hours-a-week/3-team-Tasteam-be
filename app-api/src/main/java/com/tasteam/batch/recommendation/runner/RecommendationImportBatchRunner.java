package com.tasteam.batch.recommendation.runner;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportFacade;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportFacadeCommand;
import com.tasteam.domain.recommendation.importer.RecommendationResultImportResult;

import lombok.extern.slf4j.Slf4j;

/**
 * 추천 결과 import 온디맨드 배치 진입점.
 * 동일 모델 버전에 대해 중복 실행을 방지한다.
 */
@Slf4j
@Component
public class RecommendationImportBatchRunner {

	private final RecommendationResultImportFacade importFacade;
	private final Set<String> runningModelVersions = ConcurrentHashMap.newKeySet();

	public RecommendationImportBatchRunner(RecommendationResultImportFacade importFacade) {
		this.importFacade = importFacade;
	}

	public RecommendationResultImportResult runOnDemand(String modelVersion, String s3PrefixOrUri, String requestId) {
		if (!runningModelVersions.add(modelVersion)) {
			throw RecommendationBusinessException.resultValidationFailed(
				"동일 모델 버전 import가 이미 실행 중입니다. modelVersion=" + modelVersion);
		}
		try {
			log.info("recommendation import batch runner started. modelVersion={}, requestId={}, s3PrefixOrUri={}",
				modelVersion, requestId, s3PrefixOrUri);
			return importFacade.importResults(
				new RecommendationResultImportFacadeCommand(modelVersion, s3PrefixOrUri, requestId));
		} finally {
			runningModelVersions.remove(modelVersion);
		}
	}
}
