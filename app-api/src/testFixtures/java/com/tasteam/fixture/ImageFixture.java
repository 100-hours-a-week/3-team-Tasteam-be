package com.tasteam.fixture;

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
}
