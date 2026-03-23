package com.tasteam.batch.analytics.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.domain.analytics.config.AnalyticsProperties;
import com.tasteam.infra.storage.StorageClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Kafka S3 Sink Connector가 전날 파티션 적재를 완료한 뒤 {@code _SUCCESS} 마커 파일을 생성하는 스케줄러.
 *
 * <p>Kafka Connect는 _SUCCESS 파일을 직접 생성하지 않으므로,
 * grace period(익일 00:30 KST)가 지난 후 이 스케줄러가 마커를 생성한다.
 * ML 파이프라인은 {@code _SUCCESS} 존재 여부로 해당 dt 파티션의 적재 완료를 판정한다.
 *
 * <p>대상 경로: {@code raw/events/dt=YYYY-MM-DD/_SUCCESS}
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.batch.user-activity-s3-success-marker", name = "enabled", havingValue = "true")
public class UserActivityS3SuccessMarkerScheduler {

	private static final String EVENTS_PREFIX = "raw/events/";
	private static final String SUCCESS_FILE_NAME = "_SUCCESS";
	private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

	private final StorageClient storageClient;
	private String analyticsBucket;

	@Autowired(required = false)
	void setAnalyticsProperties(AnalyticsProperties analyticsProperties) {
		if (analyticsProperties != null && StringUtils.hasText(analyticsProperties.getBucket())) {
			this.analyticsBucket = analyticsProperties.getBucket();
			log.info("user-activity s3 success marker bucket resolved. bucket={}", analyticsBucket);
		}
	}

	@Scheduled(cron = "${tasteam.batch.user-activity-s3-success-marker.cron:0 30 0 * * ?}", zone = "${tasteam.batch.user-activity-s3-success-marker.zone:Asia/Seoul}")
	public void createSuccessMarker() {
		LocalDate yesterday = LocalDate.now(KST_ZONE).minusDays(1);
		String partitionPrefix = EVENTS_PREFIX + "dt=" + yesterday + "/";
		String successKey = partitionPrefix + SUCCESS_FILE_NAME;

		log.info("user-activity s3 success marker scheduler started. dt={}", yesterday);

		var objects = listObjects(partitionPrefix);
		if (objects.isEmpty()) {
			log.warn("user-activity s3 partition에 파일이 없어 _SUCCESS 생성을 건너뜁니다. prefix={}", partitionPrefix);
			return;
		}

		uploadObject(successKey, new byte[0], "text/plain");
		log.info("user-activity s3 _SUCCESS 생성 완료. key={}, fileCount={}", successKey, objects.size());
	}

	private java.util.List<String> listObjects(String prefix) {
		if (StringUtils.hasText(analyticsBucket)) {
			return storageClient.listObjects(analyticsBucket, prefix);
		}
		return storageClient.listObjects(prefix);
	}

	private void uploadObject(String objectKey, byte[] data, String contentType) {
		if (StringUtils.hasText(analyticsBucket)) {
			storageClient.uploadObject(analyticsBucket, objectKey, data, contentType);
			return;
		}
		storageClient.uploadObject(objectKey, data, contentType);
	}
}
