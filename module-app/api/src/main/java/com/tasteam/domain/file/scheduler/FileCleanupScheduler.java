package com.tasteam.domain.file.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.domain.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupScheduler {

	private final FileService fileService;

	@Scheduled(fixedDelayString = "${tasteam.file.cleanup.fixed-delay-ms:3600000}")
	public void cleanupPendingImages() {
		int cleaned = fileService.cleanupPendingDeletedImages();
		if (cleaned > 0) {
			log.info("File cleanup completed: {} images", cleaned);
		}
	}
}
