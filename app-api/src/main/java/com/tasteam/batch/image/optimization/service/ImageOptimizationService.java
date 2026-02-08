package com.tasteam.batch.image.optimization.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import com.tasteam.batch.image.optimization.entity.ImageOptimizationJob;
import com.tasteam.batch.image.optimization.repository.ImageOptimizationJobRepository;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.infra.storage.StorageClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageOptimizationService {

	private static final int PROFILE_SIZE = 100;
	private static final int RESTAURANT_MAX_WIDTH = 2048;
	private static final int REVIEW_MAX_WIDTH = 768;
	private static final int MENU_MAX_WIDTH = 768;
	private static final double DEFAULT_QUALITY = 0.85;
	private static final String WEBP_FORMAT = "webp";
	private static final String WEBP_CONTENT_TYPE = "image/webp";
	private static final int DEFAULT_BATCH_SIZE = 100;

	private final ImageOptimizationJobRepository optimizationJobRepository;
	private final ImageRepository imageRepository;
	private final StorageClient storageClient;

	public OptimizationResult processOptimizationBatch() {
		return processOptimizationBatch(DEFAULT_BATCH_SIZE);
	}

	public OptimizationResult processOptimizationBatch(int batchSize) {
		List<DomainImage> unoptimizedDomainImages = findUnoptimizedDomainImages(batchSize);

		int successCount = 0;
		int failedCount = 0;
		int skippedCount = 0;

		for (DomainImage domainImage : unoptimizedDomainImages) {
			try {
				OptimizationOutcome outcome = processSingleDomainImage(domainImage);
				switch (outcome) {
					case SUCCESS -> successCount++;
					case SKIPPED -> skippedCount++;
					case FAILED -> failedCount++;
				}
			} catch (Exception e) {
				log.error("Unexpected error during optimization for domainImage {}: {}", domainImage.getId(),
					e.getMessage());
				failedCount++;
			}
		}

		return new OptimizationResult(successCount, failedCount, skippedCount);
	}

	@Transactional(readOnly = true)
	public List<DomainImage> findUnoptimizedDomainImages(int batchSize) {
		return optimizationJobRepository.findUnoptimizedDomainImages(PageRequest.of(0, batchSize));
	}

	@Transactional
	public OptimizationOutcome processSingleDomainImage(DomainImage domainImage) {
		Image originalImage = domainImage.getImage();
		ImageOptimizationJob job = ImageOptimizationJob.createPending(originalImage);
		optimizationJobRepository.save(job);

		try {
			return optimizeAndReplaceImage(domainImage, originalImage, job);
		} catch (Exception e) {
			log.error("Unexpected error during optimization for image {}: {}", originalImage.getId(), e.getMessage());
			job.markFailed("Unexpected error: " + e.getMessage());
			return OptimizationOutcome.FAILED;
		}
	}

	private OptimizationOutcome optimizeAndReplaceImage(DomainImage domainImage, Image originalImage,
		ImageOptimizationJob job) {
		try {
			byte[] originalData = storageClient.downloadObject(originalImage.getStorageKey());
			BufferedImage bufferedOriginal = ImageIO.read(new ByteArrayInputStream(originalData));

			if (bufferedOriginal == null) {
				job.markFailed("Unable to read image data");
				return OptimizationOutcome.FAILED;
			}

			int originalWidth = bufferedOriginal.getWidth();
			int originalHeight = bufferedOriginal.getHeight();
			long originalSize = originalData.length;

			if (!needsOptimization(originalImage, originalWidth, originalHeight)) {
				job.markSkipped("Image already meets optimization criteria");
				return OptimizationOutcome.SKIPPED;
			}

			byte[] optimizedData = processImage(originalImage.getPurpose(), bufferedOriginal, originalWidth,
				originalHeight);
			BufferedImage bufferedOptimized = ImageIO.read(new ByteArrayInputStream(optimizedData));

			int optimizedWidth = bufferedOptimized.getWidth();
			int optimizedHeight = bufferedOptimized.getHeight();
			long optimizedSize = optimizedData.length;

			UUID newUuid = UUID.randomUUID();
			String newStorageKey = generateOptimizedStorageKey(originalImage.getPurpose(), newUuid);
			storageClient.uploadObject(newStorageKey, optimizedData, WEBP_CONTENT_TYPE);

			String newFileName = replaceExtension(originalImage.getFileName(), WEBP_FORMAT);
			Image newImage = Image.create(
				originalImage.getPurpose(),
				newFileName,
				optimizedSize,
				WEBP_CONTENT_TYPE,
				newStorageKey,
				newUuid);
			newImage.activate();
			imageRepository.save(newImage);

			domainImage.replaceImage(newImage);

			originalImage.markDeletedAt(Instant.now());

			job.markSuccess(originalSize, optimizedSize, originalWidth, originalHeight, optimizedWidth,
				optimizedHeight);

			log.info("Optimized image {}: {}x{} ({}KB) -> {}x{} ({}KB), new image id={}",
				originalImage.getId(),
				originalWidth, originalHeight, originalSize / 1024,
				optimizedWidth, optimizedHeight, optimizedSize / 1024,
				newImage.getId());

			return OptimizationOutcome.SUCCESS;
		} catch (IOException e) {
			log.error("Failed to optimize image {}: {}", originalImage.getId(), e.getMessage());
			job.markFailed("IO error: " + e.getMessage());
			return OptimizationOutcome.FAILED;
		}
	}

	private boolean needsOptimization(Image image, int width, int height) {
		FilePurpose purpose = image.getPurpose();
		String fileType = image.getFileType();

		boolean isWebp = "image/webp".equalsIgnoreCase(fileType);

		return switch (purpose) {
			case PROFILE_IMAGE, GROUP_IMAGE -> !isWebp || width > PROFILE_SIZE || height > PROFILE_SIZE;
			case RESTAURANT_IMAGE -> !isWebp || width > RESTAURANT_MAX_WIDTH;
			case REVIEW_IMAGE, MENU_IMAGE -> !isWebp || width > REVIEW_MAX_WIDTH;
			case COMMON_ASSET -> !isWebp;
		};
	}

	private byte[] processImage(FilePurpose purpose, BufferedImage image, int width, int height) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		switch (purpose) {
			case PROFILE_IMAGE, GROUP_IMAGE -> processProfileImage(image, width, height, outputStream);
			case RESTAURANT_IMAGE -> processRestaurantImage(image, width, outputStream);
			case REVIEW_IMAGE, MENU_IMAGE -> processReviewImage(image, width, outputStream);
			case COMMON_ASSET -> processCommonImage(image, outputStream);
		}

		return outputStream.toByteArray();
	}

	private void processProfileImage(BufferedImage image, int width, int height, ByteArrayOutputStream out)
		throws IOException {
		int size = Math.min(width, height);
		Thumbnails.of(image)
			.sourceRegion(Positions.CENTER, size, size)
			.size(PROFILE_SIZE, PROFILE_SIZE)
			.outputFormat(WEBP_FORMAT)
			.outputQuality(DEFAULT_QUALITY)
			.toOutputStream(out);
	}

	private void processRestaurantImage(BufferedImage image, int width, ByteArrayOutputStream out) throws IOException {
		if (width > RESTAURANT_MAX_WIDTH) {
			Thumbnails.of(image)
				.width(RESTAURANT_MAX_WIDTH)
				.keepAspectRatio(true)
				.outputFormat(WEBP_FORMAT)
				.outputQuality(DEFAULT_QUALITY)
				.toOutputStream(out);
		} else {
			Thumbnails.of(image)
				.scale(1.0)
				.outputFormat(WEBP_FORMAT)
				.outputQuality(DEFAULT_QUALITY)
				.toOutputStream(out);
		}
	}

	private void processReviewImage(BufferedImage image, int width, ByteArrayOutputStream out) throws IOException {
		if (width > REVIEW_MAX_WIDTH) {
			Thumbnails.of(image)
				.width(REVIEW_MAX_WIDTH)
				.keepAspectRatio(true)
				.outputFormat(WEBP_FORMAT)
				.outputQuality(DEFAULT_QUALITY)
				.toOutputStream(out);
		} else {
			Thumbnails.of(image)
				.scale(1.0)
				.outputFormat(WEBP_FORMAT)
				.outputQuality(DEFAULT_QUALITY)
				.toOutputStream(out);
		}
	}

	private void processCommonImage(BufferedImage image, ByteArrayOutputStream out) throws IOException {
		Thumbnails.of(image)
			.scale(1.0)
			.outputFormat(WEBP_FORMAT)
			.outputQuality(DEFAULT_QUALITY)
			.toOutputStream(out);
	}

	private String generateOptimizedStorageKey(FilePurpose purpose, UUID uuid) {
		String folder = switch (purpose) {
			case PROFILE_IMAGE -> "profile";
			case GROUP_IMAGE -> "group";
			case RESTAURANT_IMAGE -> "restaurant";
			case REVIEW_IMAGE -> "review";
			case MENU_IMAGE -> "menu";
			case COMMON_ASSET -> "common";
		};
		return folder + "/" + uuid + ".webp";
	}

	private String replaceExtension(String fileName, String newExtension) {
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot > 0) {
			return fileName.substring(0, lastDot) + "." + newExtension;
		}
		return fileName + "." + newExtension;
	}

	public enum OptimizationOutcome {
		SUCCESS, FAILED, SKIPPED
	}

	public record OptimizationResult(int successCount, int failedCount, int skippedCount) {
	}
}
