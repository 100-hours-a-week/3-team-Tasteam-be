package com.tasteam.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadFileRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageUrlResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.FileErrorCode;
import com.tasteam.infra.storage.PresignedPostResponse;
import com.tasteam.infra.storage.StorageClient;

@ServiceIntegrationTest
@Transactional
class FileServiceIntegrationTest {

	private static final UUID FILE_UUID = UUID.fromString("11111111-aaaa-bbbb-cccc-111111111111");
	private static final UUID FILE_UUID_MISSING = UUID.fromString("22222222-bbbb-cccc-dddd-222222222222");

	@Autowired
	private FileService fileService;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	@MockitoBean
	private StorageClient storageClient;

	@BeforeEach
	void setUp() {
		given(storageClient.createPresignedPost(any()))
			.willReturn(new PresignedPostResponse(
				"https://upload.test",
				Map.of("key", "value"),
				Instant.now().plusSeconds(300)));
		given(storageClient.createPresignedGetUrl(anyString()))
			.willReturn("https://cdn.test/image.png");
	}

	@Nested
	@DisplayName("Presigned 업로드 생성")
	class CreatePresignedUploads {

		@Test
		@DisplayName("업로드 정책에 맞는 파일은 presigned URL이 생성된다")
		void createPresignedUploadsSuccess() {
			var request = new PresignedUploadRequest(
				FilePurpose.REVIEW_IMAGE,
				List.of(new PresignedUploadFileRequest("a.png", "image/png", 1024L)));

			PresignedUploadResponse response = fileService.createPresignedUploads(request);

			assertThat(response.uploads()).hasSize(1);
			assertThat(response.uploads().getFirst().url()).isEqualTo("https://upload.test");
			assertThat(imageRepository.findAll()).hasSize(1);
		}

		@Test
		@DisplayName("업로드 정책 위반(파일 크기)이면 실패한다")
		void createPresignedUploadsInvalidSizeFails() {
			var request = new PresignedUploadRequest(
				FilePurpose.REVIEW_IMAGE,
				List.of(new PresignedUploadFileRequest("a.png", "image/png", 0L)));

			assertThatThrownBy(() -> fileService.createPresignedUploads(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(CommonErrorCode.INVALID_REQUEST.name());
		}
	}

	@Nested
	@DisplayName("도메인 이미지 연결")
	class LinkDomainImage {

		@Test
		@DisplayName("PENDING 이미지가 ACTIVE로 전환되고 DomainImage가 생성된다")
		void linkDomainImageSuccess() {
			createAndSaveImage(FILE_UUID, "uploads/test/link.png");

			DomainImageLinkResponse response = fileService.linkDomainImage(
				new DomainImageLinkRequest(DomainType.RESTAURANT, 1L, FILE_UUID.toString(), 0));

			Image image = imageRepository.findByFileUuid(FILE_UUID).orElseThrow();
			assertThat(response.imageStatus()).isEqualTo(ImageStatus.ACTIVE.name());
			assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.RESTAURANT, List.of(1L));
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("존재하지 않는 파일 UUID면 실패한다")
		void linkDomainImageMissingFileFails() {
			assertThatThrownBy(() -> fileService.linkDomainImage(
				new DomainImageLinkRequest(DomainType.RESTAURANT, 1L, FILE_UUID_MISSING.toString(), 0)))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("이미지 URL 조회")
	class GetImageUrl {

		@Test
		@DisplayName("ACTIVE 이미지면 URL이 반환된다")
		void getImageUrlSuccess() {
			createAndSaveImage(FILE_UUID, "uploads/test/url.png");
			Image image = imageRepository.findByFileUuid(FILE_UUID).orElseThrow();
			image.activate();
			imageRepository.save(image);

			ImageUrlResponse response = fileService.getImageUrl(FILE_UUID.toString());

			assertThat(response.url()).contains("uploads/test/url.png");
		}

		@Test
		@DisplayName("존재하지 않는 파일 UUID면 실패한다")
		void getImageUrlMissingFileFails() {
			assertThatThrownBy(() -> fileService.getImageUrl(FILE_UUID_MISSING.toString()))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("이미지 정리")
	class CleanupImages {

		@Test
		@DisplayName("만료된 PENDING 이미지가 DELETED 처리되고 스토리지 삭제가 호출된다")
		void cleanupPendingDeletedImagesSuccess() {
			createAndSaveImage(FILE_UUID, "uploads/test/cleanup.png");
			Image image = imageRepository.findByFileUuid(FILE_UUID).orElseThrow();
			image.markDeletedAt(Instant.now().minusSeconds(10));
			imageRepository.save(image);

			int cleaned = fileService.cleanupPendingDeletedImages();

			Image updated = imageRepository.findByFileUuid(FILE_UUID).orElseThrow();
			assertThat(cleaned).isEqualTo(1);
			assertThat(updated.getStatus()).isEqualTo(ImageStatus.DELETED);
		}
	}

	private void createAndSaveImage(UUID fileUuid, String storageKey) {
		imageRepository.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, storageKey, fileUuid));
	}
}
