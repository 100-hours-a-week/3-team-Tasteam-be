package com.tasteam.infra.messagequeue;

import java.time.Instant;

/**
 * S3 Data Lake 적재를 위한 사용자 행동 이벤트 데이터 계약.
 *
 * <p>Kafka Connect S3 Sink Connector가 이 레코드를 CSV 행으로 직렬화한다.
 * 필드 순서가 CSV 컬럼 순서이므로 변경 시 스키마 버전을 함께 올려야 한다.
 *
 * @param eventId          전역 이벤트 식별자
 * @param eventName        이벤트 이름 (예: ui.restaurant.clicked)
 * @param eventVersion     이벤트 스키마 버전 (예: v1)
 * @param occurredAt       이벤트 발생 시각 (ISO-8601); S3 파티션 기준 타임스탬프
 * @param diningType       식사 유형 (예: DINE_IN, DELIVERY)
 * @param distanceBucket   거리 버킷 (예: NEAR, MID, FAR)
 * @param weatherBucket    날씨 버킷 (예: CLEAR, RAIN, SNOW)
 * @param memberId         인증 사용자 식별자 (익명 이벤트면 null)
 * @param anonymousId      익명 사용자 식별자 (인증 이벤트면 null)
 * @param sessionId        세션 식별자
 * @param restaurantId     레스토랑 식별자 (해당 없으면 null)
 * @param recommendationId 추천 컨텍스트 식별자 (해당 없으면 null)
 * @param platform         클라이언트 플랫폼 (예: IOS, ANDROID, WEB)
 * @param createdAt        서버 수신 시각 (publisher가 Instant.now()로 설정)
 */
public record UserActivityS3Event(
	String eventId,
	String eventName,
	String eventVersion,
	Instant occurredAt,
	String diningType,
	String distanceBucket,
	String weatherBucket,
	Long memberId,
	String anonymousId,
	String sessionId,
	Long restaurantId,
	String recommendationId,
	String platform,
	Instant createdAt) {
}
