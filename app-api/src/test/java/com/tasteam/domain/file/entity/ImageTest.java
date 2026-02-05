package com.tasteam.domain.file.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("이미지 엔티티")
class ImageTest {

	private static final FilePurpose DEFAULT_PURPOSE = FilePurpose.REVIEW_IMAGE;
	private static final String DEFAULT_FILE_NAME = "테스트파일.png";
	private static final long DEFAULT_FILE_SIZE = 1024L;
	private static final String DEFAULT_FILE_TYPE = "image/png";
	private static final String DEFAULT_STORAGE_KEY = "uploads/review/test.png";
	private static final UUID DEFAULT_FILE_UUID = UUID.randomUUID();

	@Nested
	@DisplayName("이미지 생성")
	class CreateImage {

		@Test
		@DisplayName("유효한 파라미터로 이미지를 생성하면 PENDING 상태로 생성된다")
		void create_validParams_createsPendingImage() {
			Image image = Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID);

			assertThat(image.getPurpose()).isEqualTo(DEFAULT_PURPOSE);
			assertThat(image.getFileName()).isEqualTo(DEFAULT_FILE_NAME);
			assertThat(image.getFileSize()).isEqualTo(DEFAULT_FILE_SIZE);
			assertThat(image.getFileType()).isEqualTo(DEFAULT_FILE_TYPE);
			assertThat(image.getStorageKey()).isEqualTo(DEFAULT_STORAGE_KEY);
			assertThat(image.getFileUuid()).isEqualTo(DEFAULT_FILE_UUID);
			assertThat(image.getStatus()).isEqualTo(ImageStatus.PENDING);
		}

		@Test
		@DisplayName("파일 목적이 null이면 이미지 생성에 실패한다")
		void create_nullPurpose_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(null, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("파일 이름이 빈 문자열이면 이미지 생성에 실패한다")
		void create_blankFileName_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, "  ", DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("파일 이름이 최대 길이를 초과하면 이미지 생성에 실패한다")
		void create_fileNameTooLong_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, "a".repeat(257), DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("파일 타입이 빈 문자열이면 이미지 생성에 실패한다")
		void create_blankFileType_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				"", DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("파일 타입이 최대 길이를 초과하면 이미지 생성에 실패한다")
		void create_fileTypeTooLong_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				"a".repeat(65), DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("스토리지 키가 빈 문자열이면 이미지 생성에 실패한다")
		void create_blankStorageKey_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, "", DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("fileUuid가 null이면 이미지 생성에 실패한다")
		void create_nullFileUuid_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, null))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("파일 크기가 0이면 이미지 생성에 실패한다")
		void create_zeroFileSize_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, 0L,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("파일 크기가 음수이면 이미지 생성에 실패한다")
		void create_negativeFileSize_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, -1L,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("이미지 상태 전이")
	class ImageStatusTransition {

		@Test
		@DisplayName("이미지를 활성화하면 상태가 ACTIVE로 변경된다")
		void activate_changesStatusToActive() {
			Image image = Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID);

			image.activate();

			assertThat(image.getStatus()).isEqualTo(com.tasteam.domain.file.entity.ImageStatus.ACTIVE);
		}

		@Test
		@DisplayName("이미지를 정리하면 상태가 DELETED로 변경된다")
		void cleanup_changesStatusToDeleted() {
			Image image = Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID);

			image.cleanup();

			assertThat(image.getStatus()).isEqualTo(com.tasteam.domain.file.entity.ImageStatus.DELETED);
		}

		@Test
		@DisplayName("삭제 예정 시간을 표시하면 deletedAt이 설정된다")
		void markDeletedAt_setsDeletedAt() {
			Image image = Image.create(DEFAULT_PURPOSE, DEFAULT_FILE_NAME, DEFAULT_FILE_SIZE,
				DEFAULT_FILE_TYPE, DEFAULT_STORAGE_KEY, DEFAULT_FILE_UUID);
			Instant deletedAt = Instant.parse("2026-06-01T00:00:00Z");

			image.markDeletedAt(deletedAt);

			assertThat(image.getDeletedAt()).isEqualTo(deletedAt);
		}
	}
}
