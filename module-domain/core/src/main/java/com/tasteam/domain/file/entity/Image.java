package com.tasteam.domain.file.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.util.Assert;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "image")
public class Image extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_seq_gen")
	@SequenceGenerator(name = "image_seq_gen", sequenceName = "image_seq", allocationSize = 50)
	private Long id;

	@Column(name = "file_name", nullable = false, length = 256)
	private String fileName;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(name = "file_type", nullable = false, length = 64)
	private String fileType;

	@Column(name = "storage_key", nullable = false, unique = true, length = 512)
	private String storageKey;

	@Column(name = "file_uuid", nullable = false, unique = true)
	private UUID fileUuid;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private ImageStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "purpose", nullable = false, length = 32)
	private FilePurpose purpose;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static Image create(FilePurpose purpose, String fileName, long fileSize, String fileType, String storageKey,
		UUID fileUuid) {
		validateCreate(purpose, fileName, fileSize, fileType, storageKey, fileUuid);
		return Image.builder()
			.purpose(purpose)
			.fileName(fileName)
			.fileSize(fileSize)
			.fileType(fileType)
			.storageKey(storageKey)
			.fileUuid(fileUuid)
			.status(ImageStatus.PENDING)
			.build();
	}

	public void activate() {
		this.status = ImageStatus.ACTIVE;
	}

	public void markDeletedAt(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public void cleanup() {
		this.status = ImageStatus.DELETED;
	}

	private static void validateCreate(FilePurpose purpose, String fileName, long fileSize, String fileType,
		String storageKey, UUID fileUuid) {
		Assert.notNull(purpose, "파일 목적은 필수입니다");
		Assert.hasText(fileName, "파일 이름은 필수입니다");
		Assert.hasText(fileType, "파일 타입은 필수입니다");
		Assert.hasText(storageKey, "스토리지 키는 필수입니다");
		Assert.notNull(fileUuid, "fileUuid는 필수입니다");
		if (fileName.length() > 256) {
			throw new IllegalArgumentException("파일 이름이 너무 깁니다");
		}
		if (fileType.length() > 64) {
			throw new IllegalArgumentException("파일 타입이 너무 깁니다");
		}
		if (fileSize <= 0) {
			throw new IllegalArgumentException("파일 크기는 양수여야 합니다");
		}
	}
}
