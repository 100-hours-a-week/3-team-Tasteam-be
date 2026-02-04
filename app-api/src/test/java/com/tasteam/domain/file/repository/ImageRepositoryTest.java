package com.tasteam.domain.file.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.fixture.ImageFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("ImageRepository 테스트")
class ImageRepositoryTest {

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("이미지를 저장하면 기본 매핑이 정상이고 status가 PENDING이다")
	void saveAndFind() {
		UUID fileUuid = UUID.randomUUID();
		Image image = ImageFixture.create(FilePurpose.REVIEW_IMAGE, "test/key-001", fileUuid);

		Image saved = imageRepository.save(image);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getFileUuid()).isEqualTo(fileUuid);
		assertThat(saved.getStatus()).isEqualTo(ImageStatus.PENDING);
		assertThat(saved.getPurpose()).isEqualTo(FilePurpose.REVIEW_IMAGE);
	}

	@Test
	@DisplayName("findByFileUuid - UUID로 저장된 이미지를 조회 성공")
	void findByFileUuid_success() {
		UUID fileUuid = UUID.randomUUID();
		imageRepository.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, "test/key-002", fileUuid));
		entityManager.flush();
		entityManager.clear();

		var result = imageRepository.findByFileUuid(fileUuid);

		assertThat(result).isPresent();
		assertThat(result.get().getFileUuid()).isEqualTo(fileUuid);
	}

	@Test
	@DisplayName("findByFileUuidAndStatus - 상태가 일치하지 않으면 조회되지 않는다")
	void findByFileUuidAndStatus_excludesMismatchedStatus() {
		UUID fileUuid = UUID.randomUUID();
		imageRepository.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, "test/key-003", fileUuid));
		entityManager.flush();
		entityManager.clear();

		var result = imageRepository.findByFileUuidAndStatus(fileUuid, ImageStatus.ACTIVE);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("save - 동일한 fileUuid를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateFileUuid_throwsDataIntegrityViolationException() {
		UUID duplicateUuid = UUID.randomUUID();
		imageRepository.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, "test/key-dup-a", duplicateUuid));
		imageRepository.save(ImageFixture.create(FilePurpose.REVIEW_IMAGE, "test/key-dup-b", duplicateUuid));

		assertThatThrownBy(() -> entityManager.flush())
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
