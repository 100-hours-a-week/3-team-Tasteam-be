package com.tasteam.batch.analytics.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.export.RawDataExportCommand;
import com.tasteam.domain.analytics.export.RawDataExportService;
import com.tasteam.domain.analytics.export.RawDataType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.batch.raw-export", name = "enabled", havingValue = "true")
public class RawDataExportScheduler {

	private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

	private final RawDataExportService rawDataExportService;

	@Scheduled(cron = "${tasteam.batch.raw-export.cron:0 15 2 * * ?}", zone = "${tasteam.batch.raw-export.zone:Asia/Seoul}")
	public void runDaily() {
		LocalDate dt = LocalDate.now(KST_ZONE);
		String requestId = "raw-export-scheduled-" + UUID.randomUUID();
		log.info("raw data export scheduler started. dt={}, requestId={}", dt, requestId);
		rawDataExportService.export(
			new RawDataExportCommand(dt, EnumSet.allOf(RawDataType.class), requestId));
	}
}
