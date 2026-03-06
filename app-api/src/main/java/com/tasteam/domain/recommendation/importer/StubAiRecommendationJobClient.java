package com.tasteam.domain.recommendation.importer;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * O2 단계용 Stub 구현체.
 * 실제 AI 서버 연동 전까지 요청 수락 로그와 가짜 jobId를 반환한다.
 */
@Component
public class StubAiRecommendationJobClient implements AiRecommendationJobClient {

	private static final Logger log = LoggerFactory.getLogger(StubAiRecommendationJobClient.class);

	@Override
	public AiRecommendationResponse requestRecommendation(AiRecommendationRequest request) {
		String jobId = "stub-" + request.modelVersion() + "-" + Instant.now().toEpochMilli();
		log.info("stub ai request accepted. modelVersion={}, requestId={}, jobId={}",
			request.modelVersion(),
			request.requestId(),
			jobId);
		return new AiRecommendationResponse(jobId);
	}
}
