package com.tasteam.fixture;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;

import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;

public final class ImageFixture {

	public static final String DEFAULT_FILE_NAME = "profile.png";
	public static final long DEFAULT_FILE_SIZE = 1024L;
	public static final String DEFAULT_FILE_TYPE = "image/png";

	private ImageFixture() {}

	public static Image create(FilePurpose purpose, String storageKey, UUID fileUuid) {
		return Image.create(purpose, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE, DEFAULT_FILE_TYPE, storageKey, fileUuid);
	}

	public static Image create(FilePurpose purpose, String storageKey, UUID fileUuid, String fileName) {
		return Image.create(purpose, fileName, DEFAULT_FILE_SIZE, DEFAULT_FILE_TYPE, storageKey, fileUuid);
	}

	public static Image create(FilePurpose purpose, String storageKey, UUID fileUuid, String fileName, long fileSize) {
		return Image.create(purpose, fileName, fileSize, DEFAULT_FILE_TYPE, storageKey, fileUuid);
	}

	public static Image createWithCreatedAt(FilePurpose purpose, String storageKey, UUID fileUuid, Instant createdAt) {
		Image image = create(purpose, storageKey, fileUuid);
		setCreatedAt(image, createdAt);
		return image;
	}

	private static void setCreatedAt(Image image, Instant createdAt) {
		try {
			Field field = image.getClass().getSuperclass().getDeclaredField("createdAt");
			field.setAccessible(true);
			field.set(image, createdAt);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("createdAt 필드 설정 실패", e);
		}
	}
}
