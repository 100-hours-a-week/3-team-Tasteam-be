package com.tasteam.domain.analytics.export;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawDataExportAsyncLauncher {

	private final RawDataExportService rawDataExportService;

	@Async("rawDataExportExecutor")
	public void launch(RawDataExportCommand command) {
		Assert.notNull(command, "command는 null일 수 없습니다.");
		try {
			rawDataExportService.export(command);
		} catch (RuntimeException ex) {
			log.error("raw data export async execution failed. requestId={}, dt={}, targets={}",
				command.requestId(), command.dt(), command.targets(), ex);
		}
	}
}
