package com.tasteam.batch.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.repository.AiJobRepository;

import lombok.RequiredArgsConstructor;

/**
 * AI Job 선점: PENDING → RUNNING. 벡터 업로드·리뷰 감정 등 AI 관련 배치에서 공통 사용.
 * DB UPDATE만으로 동시성 보장, 앱 레벨 락 없음.
 */
@Service
@RequiredArgsConstructor
public class AiJobClaimService {

	private final AiJobRepository aiJobRepository;

	/**
	 * PENDING인 Job을 RUNNING으로 선점. REQUIRES_NEW로 독립 트랜잭션에서 실행.
	 *
	 * @param jobId AiJob.id
	 * @return 선점 성공 시 true, 이미 다른 워커가 선점했거나 상태가 PENDING이 아니면 false
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean tryClaimToRunning(Long jobId) {
		int updated = aiJobRepository.claimToRunningIfPending(
			jobId, AiJobStatus.PENDING, AiJobStatus.RUNNING);
		return updated == 1;
	}
}
