package com.tasteam.batch.analytics.scheduler;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.export.RawDataExportCommand;
import com.tasteam.domain.analytics.export.RawDataExportService;
import com.tasteam.domain.analytics.export.RawDataType;
import com.tasteam.global.lock.RedisDistributedLockManager;
import com.tasteam.global.lock.RedisDistributedLockManager.LockHandle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.batch.raw-export", name = "enabled", havingValue = "true")
public class RawDataExportScheduler {

	private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
	private static final String LOCK_KEY = "lock:batch:analytics:raw-export";
	private static final Duration LOCK_TTL = Duration.ofMinutes(25);

	private final RawDataExportService rawDataExportService;
	private final RedisDistributedLockManager distributedLockManager;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		runExport("startup");
	}

	@Scheduled(cron = "${tasteam.batch.raw-export.cron:0 15,45 * * * ?}", zone = "${tasteam.batch.raw-export.zone:Asia/Seoul}")
	public void runDaily() {
		runExport("scheduled");
	}

	private void runExport(String trigger) {
		Optional<LockHandle> lockHandleOpt = distributedLockManager.tryLock(LOCK_KEY, LOCK_TTL);
		if (lockHandleOpt.isEmpty()) {
			log.info("raw data export scheduler skipped. trigger={}, another instance already owns lock. key={}",
				trigger, LOCK_KEY);
			return;
		}

		LocalDate dt = LocalDate.now(KST_ZONE);
		String requestId = "raw-export-" + trigger + "-" + UUID.randomUUID();
		try (LockHandle ignored = lockHandleOpt.get()) {
			log.info("raw data export scheduler started. trigger={}, dt={}, requestId={}", trigger, dt, requestId);
			rawDataExportService.export(
				new RawDataExportCommand(dt, EnumSet.allOf(RawDataType.class), requestId));
		}
	}
}
