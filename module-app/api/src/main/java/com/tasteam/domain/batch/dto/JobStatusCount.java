package com.tasteam.domain.batch.dto;

import com.tasteam.domain.batch.entity.AiJobStatus;

/**
 * 배치 실행 내 Job의 status별 개수 (DB 집계 프로젝션용).
 */
public record JobStatusCount(AiJobStatus status, long count) {
}
