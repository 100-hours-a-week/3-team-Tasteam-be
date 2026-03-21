package com.tasteam.domain.batch.dto;

import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.AiJobType;

/**
 * 배치 실행 내 Job의 jobType+status별 개수 (DB 집계 프로젝션용).
 */
public record JobTypeStatusCount(AiJobType jobType, AiJobStatus status, long count) {
}
