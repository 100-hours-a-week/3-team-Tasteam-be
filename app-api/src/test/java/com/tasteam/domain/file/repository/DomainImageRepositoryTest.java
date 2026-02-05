package com.tasteam.domain.file.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.fixture.ImageFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("DomainImageRepository 테스트")
class DomainImageRepositoryTest {

	@Autowired
	private DomainImageRepository domainImageRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private EntityManager entityManager;

	private Image saveActiveImage(String storageKey) {
		Image image = ImageFixture.create(FilePurpose.REVIEW_IMAGE, storageKey, UUID.randomUUID());
		image = imageRepository.save(image);
		image.activate();
		return image;
	}

	private Image savePendingImage(String storageKey) {
		return imageRepository.save(
			ImageFixture.create(FilePurpose.REVIEW_IMAGE, storageKey, UUID.randomUUID()));
	}

	@Test
	@DisplayName("DomainImage를 저장하면 기본 매핑과 연관관계가 정상이다")
	void saveAndFind() {
		Image image = saveActiveImage("test/domain-save-001");
		DomainImage domainImage = DomainImage.create(DomainType.REVIEW, 100L, image, 0);

		DomainImage saved = domainImageRepository.save(domainImage);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getDomainType()).isEqualTo(DomainType.REVIEW);
		assertThat(saved.getDomainId()).isEqualTo(100L);
		assertThat(saved.getImage().getId()).isEqualTo(image.getId());
	}

	@Test
	@DisplayName("findAllByDomainTypeAndDomainIdIn - ACTIVE 이미지만 반환되고 PENDING 이미지는 제외된다")
	void findAllByDomainTypeAndDomainIdIn_onlyActiveImages() {
		Image activeImage = saveActiveImage("test/domain-active-001");
		Image pendingImage = savePendingImage("test/domain-pending-001");
		domainImageRepository.save(DomainImage.create(DomainType.REVIEW, 200L, activeImage, 0));
		domainImageRepository.save(DomainImage.create(DomainType.REVIEW, 200L, pendingImage, 1));
		entityManager.flush();
		entityManager.clear();

		List<DomainImage> results = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
			DomainType.REVIEW, List.of(200L));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getImage().getId()).isEqualTo(activeImage.getId());
	}

	@Test
	@DisplayName("findAllByDomainTypeAndDomainIdIn - sortOrder asc로 정렬된다")
	void findAllByDomainTypeAndDomainIdIn_orderBySortOrder() {
		Image image1 = saveActiveImage("test/domain-sort-a");
		Image image2 = saveActiveImage("test/domain-sort-b");
		domainImageRepository.save(DomainImage.create(DomainType.REVIEW, 300L, image1, 2));
		domainImageRepository.save(DomainImage.create(DomainType.REVIEW, 300L, image2, 1));
		entityManager.flush();
		entityManager.clear();

		List<DomainImage> results = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
			DomainType.REVIEW, List.of(300L));

		assertThat(results).hasSize(2);
		assertThat(results.get(0).getSortOrder()).isLessThanOrEqualTo(results.get(1).getSortOrder());
	}

	@Test
	@DisplayName("save - 동일한 (domainType, domainId, imageId)를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateDomainImage_throwsDataIntegrityViolationException() {
		Image image = saveActiveImage("test/domain-dup-001");
		domainImageRepository.saveAndFlush(DomainImage.create(DomainType.REVIEW, 400L, image, 0));

		assertThatThrownBy(() -> domainImageRepository.saveAndFlush(
			DomainImage.create(DomainType.REVIEW, 400L, image, 1)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
