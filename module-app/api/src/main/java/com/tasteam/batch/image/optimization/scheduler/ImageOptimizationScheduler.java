package com.tasteam.batch.image.optimization.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.batch.image.optimization.repository.ImageOptimizationJobRepository;
import com.tasteam.batch.image.optimization.service.ImageOptimizationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.image.optimization", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ImageOptimizationScheduler {

	private final ImageOptimizationService optimizationService;
	private final ImageOptimizationJobRepository optimizationJobRepository;
	private final AtomicBoolean isRunning = new AtomicBoolean(false);

	@Scheduled(cron = "${tasteam.image.optimization.cron:0 0 3 * * ?}")
	public void runOptimization() {
		if (!isRunning.compareAndSet(false, true)) {
			log.warn("Image optimization job already running, skipping this execution");
			return;
		}
		try {
			log.info("Starting image optimization batch job");
			ImageOptimizationService.OptimizationResult result = optimizationService.processOptimizationBatch();
			log.info("Image optimization completed: success={}, failed={}, skipped={}",
				result.successCount(), result.failedCount(), result.skippedCount());
		} catch (Exception e) {
			log.error("Image optimization batch job failed", e);
		} finally {
			isRunning.set(false);
		}
	}

	@Scheduled(cron = "${tasteam.image.optimization.cleanup-cron:0 0 4 * * ?}")
	public void cleanupExpiredJobs() {
		Instant cutoff = Instant.now().minus(Duration.ofDays(7));
		int deleted = optimizationJobRepository.deleteBySuccessAndProcessedAtBefore(cutoff);
		log.info("Cleaned up {} expired optimization jobs", deleted);
	}
}
