package com.tasteam.batch.analytics.scheduler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.config.AnalyticsProperties;
import com.tasteam.infra.storage.StorageClient;

@UnitTest
@DisplayName("[유닛](Batch) UserActivityS3SuccessMarkerScheduler 단위 테스트")
class UserActivityS3SuccessMarkerSchedulerTest {

	private StorageClient storageClient;
	private UserActivityS3SuccessMarkerScheduler scheduler;

	@BeforeEach
	void setUp() {
		storageClient = mock(StorageClient.class);
		scheduler = new UserActivityS3SuccessMarkerScheduler(storageClient);

		AnalyticsProperties analyticsProperties = new AnalyticsProperties();
		analyticsProperties.setBucket("tasteam-dev-analytics");
		scheduler.setAnalyticsProperties(analyticsProperties);
	}

	@Test
	@DisplayName("전날 파티션에 파일이 있으면 _SUCCESS 마커를 업로드한다")
	void createSuccessMarker_uploadsSuccessWhenFilesExist() {
		// given
		when(storageClient.listObjects(anyString(), anyString()))
			.thenReturn(List.of("raw/events/dt=2026-03-11/part-00001.csv"));

		// when
		scheduler.createSuccessMarker();

		// then
		verify(storageClient).uploadObject(
			eq("tasteam-dev-analytics"),
			anyString(),
			eq(new byte[0]),
			eq("text/plain"));
	}

	@Test
	@DisplayName("전날 파티션에 파일이 없으면 _SUCCESS 마커를 생성하지 않는다")
	void createSuccessMarker_skipsWhenNoFilesExist() {
		// given
		when(storageClient.listObjects(anyString(), anyString())).thenReturn(List.of());

		// when
		scheduler.createSuccessMarker();

		// then
		verify(storageClient, never()).uploadObject(
			anyString(), anyString(), eq(new byte[0]), anyString());
	}

	@Test
	@DisplayName("analyticsBucket이 설정되지 않으면 버킷 없는 StorageClient 메서드를 호출한다")
	void createSuccessMarker_usesStorageClientWithoutBucketWhenNotConfigured() {
		// given
		StorageClient noBucketStorage = mock(StorageClient.class);
		UserActivityS3SuccessMarkerScheduler noBucketScheduler = new UserActivityS3SuccessMarkerScheduler(
			noBucketStorage);
		when(noBucketStorage.listObjects(anyString()))
			.thenReturn(List.of("raw/events/dt=2026-03-11/part-00001.csv"));

		// when
		noBucketScheduler.createSuccessMarker();

		// then
		verify(noBucketStorage).uploadObject(anyString(), eq(new byte[0]), eq("text/plain"));
	}
}
