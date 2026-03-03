package com.tasteam.batch.ai.vector.runner;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService;
import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService.RestaurantWithReviews;
import com.tasteam.batch.ai.vector.service.VectorUploadEpochSyncService;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiVectorUploadRequest;
import com.tasteam.infra.ai.dto.AiVectorUploadResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 단일 벡터 업로드 실행: 데이터 로드 → AI 호출 → 에폭 싱크.
 * 성공 시 갱신된 vectorEpoch 반환. (배치 워커 또는 이벤트에서 호출)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorUploadRunner {

	private final VectorUploadDataLoadService dataLoadService;
	private final VectorUploadEpochSyncService epochSyncService;
	private final AiClient aiClient;

	/**
	 * Job 한 건에 대해 벡터 업로드 실행.
	 *
	 * @param job 벡터 업로드 Job (RUNNING 가정)
	 * @return 성공 시 Success(갱신된 vectorEpoch), 그 외 DataMissing / InvokeFailed / SyncSkipped
	 */
	public VectorUploadRunResult run(AiJob job) {
		var dataOpt = dataLoadService.loadByRestaurantId(job.getRestaurantId());
		if (dataOpt.isEmpty()) {
			return new VectorUploadRunResult.DataMissing();
		}

		RestaurantWithReviews data = dataOpt.get();
		VectorUploadInvokeResult invokeResult = invokeVectorUpload(data);

		return switch (invokeResult) {
			case VectorUploadInvokeResult.Success s -> {
				boolean synced = epochSyncService.syncEpochAfterUpload(job, data, Instant.now());
				if (synced) {
					yield new VectorUploadRunResult.Success(job.getBaseEpoch() + 1);
				}
				log.debug("Vector upload epoch sync skipped (concurrent), jobId={}, restaurantId={}",
					job.getId(), job.getRestaurantId());
				yield new VectorUploadRunResult.SyncSkipped();
			}
			case VectorUploadInvokeResult.Failure f -> new VectorUploadRunResult.InvokeFailed(f.cause());
		};
	}

	/**
	 * 이벤트 경로용: Job 없이 restaurantId만으로 벡터 업로드 실행.
	 * 레스토랑의 현재 vector_epoch를 기대값으로 사용해 에폭 싱크.
	 *
	 * @param restaurantId 레스토랑 ID
	 * @return 성공 시 Success(갱신된 vectorEpoch), 그 외 DataMissing / InvokeFailed / SyncSkipped
	 */
	public VectorUploadRunResult runForEvent(long restaurantId) {
		var dataOpt = dataLoadService.loadByRestaurantId(restaurantId);
		if (dataOpt.isEmpty()) {
			return new VectorUploadRunResult.DataMissing();
		}

		RestaurantWithReviews data = dataOpt.get();
		long baseEpoch = data.restaurant().getVectorEpoch() != null
			? data.restaurant().getVectorEpoch()
			: 0L;

		VectorUploadInvokeResult invokeResult = invokeVectorUpload(data);

		return switch (invokeResult) {
			case VectorUploadInvokeResult.Success s -> {
				boolean synced = epochSyncService.syncEpochAfterUpload(
					restaurantId, baseEpoch, data, Instant.now());
				if (synced) {
					yield new VectorUploadRunResult.Success(baseEpoch + 1);
				}
				log.debug("Vector upload epoch sync skipped (concurrent), restaurantId={}", restaurantId);
				yield new VectorUploadRunResult.SyncSkipped();
			}
			case VectorUploadInvokeResult.Failure f -> new VectorUploadRunResult.InvokeFailed(f.cause());
		};
	}

	private VectorUploadInvokeResult invokeVectorUpload(RestaurantWithReviews data) {
		AiVectorUploadRequest request = toRequest(data);
		try {
			AiVectorUploadResponse response = aiClient.uploadVectorData(request);
			return new VectorUploadInvokeResult.Success(response);
		} catch (Exception e) {
			return new VectorUploadInvokeResult.Failure(e);
		}
	}

	private static AiVectorUploadRequest toRequest(RestaurantWithReviews data) {
		long restaurantId = data.restaurant().getId();
		List<AiVectorUploadRequest.ReviewPayload> reviews = data.reviews().stream()
			.map(r -> new AiVectorUploadRequest.ReviewPayload(
				r.getId(),
				restaurantId,
				r.getContent() != null ? r.getContent() : "",
				r.getCreatedAt() != null ? r.getCreatedAt() : Instant.now()))
			.toList();
		List<AiVectorUploadRequest.RestaurantPayload> restaurants = List.of(
			new AiVectorUploadRequest.RestaurantPayload(
				data.restaurant().getId(),
				data.restaurant().getName()));
		return new AiVectorUploadRequest(reviews, restaurants);
	}
}
