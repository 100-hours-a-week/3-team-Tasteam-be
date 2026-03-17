package com.tasteam.batch.ai.vector.runner;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService;
import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService.RestaurantWithReviews;
import com.tasteam.batch.ai.vector.service.VectorUploadEpochSyncService;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.global.metrics.MetricLabelPolicy;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiVectorUploadRequest;
import com.tasteam.infra.ai.dto.AiVectorUploadResponse;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
	private final MeterRegistry meterRegistry;

	/**
	 * Job 한 건에 대해 벡터 업로드 실행.
	 *
	 * @param job 벡터 업로드 Job (RUNNING 가정)
	 * @return 성공 시 Success(갱신된 vectorEpoch), 그 외 DataMissing / InvokeFailed / SyncSkipped
	 */
	public VectorUploadRunResult run(AiJob job) {
		Timer.Sample totalSample = Timer.start(meterRegistry);
		String result = "invoke_failed";
		try {
			Timer.Sample loadSample = Timer.start(meterRegistry);
			var dataOpt = dataLoadService.loadByRestaurantId(job.getRestaurantId());
			recordTimer("ai.vector_upload.db_load.duration", loadSample, "path", "job", "result",
				dataOpt.isPresent() ? "found" : "not_found");
			if (dataOpt.isEmpty()) {
				result = "data_missing";
				return new VectorUploadRunResult.DataMissing();
			}

			RestaurantWithReviews data = dataOpt.get();
			recordPayloadSize(data.reviews().size(), "job");
			VectorUploadInvokeResult invokeResult = invokeVectorUpload(data);

			VectorUploadRunResult runResult = switch (invokeResult) {
				case VectorUploadInvokeResult.Success s -> {
					Timer.Sample syncSample = Timer.start(meterRegistry);
					boolean synced = epochSyncService.syncEpochAfterUpload(job, data, Instant.now());
					recordTimer("ai.vector_upload.epoch_sync.duration", syncSample, "path", "job", "result",
						synced ? "success" : "sync_skipped");
					if (synced) {
						yield new VectorUploadRunResult.Success(job.getBaseEpoch() + 1);
					}
					log.debug("Vector upload epoch sync skipped (concurrent), jobId={}, restaurantId={}",
						job.getId(), job.getRestaurantId());
					yield new VectorUploadRunResult.SyncSkipped();
				}
				case VectorUploadInvokeResult.Failure f -> new VectorUploadRunResult.InvokeFailed(f.cause());
			};
			result = toResultTag(runResult);
			return runResult;
		} finally {
			recordCounter("ai.vector_upload.execute.total", "path", "job", "result", result);
			recordTimer("ai.vector_upload.execute.duration", totalSample, "path", "job", "result", result);
		}
	}

	/**
	 * 이벤트 경로용: Job 없이 restaurantId만으로 벡터 업로드 실행.
	 * 레스토랑의 현재 vector_epoch를 기대값으로 사용해 에폭 싱크.
	 *
	 * @param restaurantId 레스토랑 ID
	 * @return 성공 시 Success(갱신된 vectorEpoch), 그 외 DataMissing / InvokeFailed / SyncSkipped
	 */
	public VectorUploadRunResult runForEvent(long restaurantId) {
		Timer.Sample totalSample = Timer.start(meterRegistry);
		String result = "invoke_failed";
		try {
			Timer.Sample loadSample = Timer.start(meterRegistry);
			var dataOpt = dataLoadService.loadByRestaurantId(restaurantId);
			recordTimer("ai.vector_upload.db_load.duration", loadSample, "path", "event", "result",
				dataOpt.isPresent() ? "found" : "not_found");
			if (dataOpt.isEmpty()) {
				result = "data_missing";
				return new VectorUploadRunResult.DataMissing();
			}

			RestaurantWithReviews data = dataOpt.get();
			recordPayloadSize(data.reviews().size(), "event");
			long baseEpoch = data.restaurant().getVectorEpoch() != null
				? data.restaurant().getVectorEpoch()
				: 0L;

			VectorUploadInvokeResult invokeResult = invokeVectorUpload(data);

			VectorUploadRunResult runResult = switch (invokeResult) {
				case VectorUploadInvokeResult.Success s -> {
					Timer.Sample syncSample = Timer.start(meterRegistry);
					boolean synced = epochSyncService.syncEpochAfterUpload(
						restaurantId, baseEpoch, data, Instant.now());
					recordTimer("ai.vector_upload.epoch_sync.duration", syncSample, "path", "event", "result",
						synced ? "success" : "sync_skipped");
					if (synced) {
						yield new VectorUploadRunResult.Success(baseEpoch + 1);
					}
					log.debug("Vector upload epoch sync skipped (concurrent), restaurantId={}", restaurantId);
					yield new VectorUploadRunResult.SyncSkipped();
				}
				case VectorUploadInvokeResult.Failure f -> new VectorUploadRunResult.InvokeFailed(f.cause());
			};
			result = toResultTag(runResult);
			return runResult;
		} finally {
			recordCounter("ai.vector_upload.execute.total", "path", "event", "result", result);
			recordTimer("ai.vector_upload.execute.duration", totalSample, "path", "event", "result", result);
		}
	}

	private VectorUploadInvokeResult invokeVectorUpload(RestaurantWithReviews data) {
		AiVectorUploadRequest request = toRequest(data);
		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			AiVectorUploadResponse response = aiClient.uploadVectorData(request);
			recordTimer("ai.vector_upload.ai_invoke.duration", sample, "result", "success");
			return new VectorUploadInvokeResult.Success(response);
		} catch (Exception e) {
			recordTimer("ai.vector_upload.ai_invoke.duration", sample, "result", "failed");
			return new VectorUploadInvokeResult.Failure(e);
		}
	}

	private void recordPayloadSize(int reviewCount, String path) {
		String metricName = "ai.vector_upload.review_payload.size";
		MetricLabelPolicy.validate(metricName, "path", path);
		DistributionSummary.builder(metricName)
			.tags("path", path)
			.register(meterRegistry)
			.record(reviewCount);
	}

	private String toResultTag(VectorUploadRunResult result) {
		return switch (result) {
			case VectorUploadRunResult.Success __ -> "success";
			case VectorUploadRunResult.DataMissing __ -> "data_missing";
			case VectorUploadRunResult.InvokeFailed __ -> "invoke_failed";
			case VectorUploadRunResult.SyncSkipped __ -> "sync_skipped";
		};
	}

	private void recordCounter(String metricName, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		meterRegistry.counter(metricName, tags).increment();
	}

	private void recordTimer(String metricName, Timer.Sample sample, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		sample.stop(Timer.builder(metricName).tags(tags).register(meterRegistry));
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
