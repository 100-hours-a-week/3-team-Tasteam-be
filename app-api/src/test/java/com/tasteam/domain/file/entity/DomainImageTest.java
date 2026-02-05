package com.tasteam.domain.file.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.fixture.ImageFixture;

@UnitTest
@DisplayName("도메인 이미지 엔티티")
class DomainImageTest {

	private static final DomainType DEFAULT_DOMAIN_TYPE = DomainType.REVIEW;
	private static final Long DEFAULT_DOMAIN_ID = 1L;

	private Image createImage() {
		return ImageFixture.create(FilePurpose.REVIEW_IMAGE, "uploads/review/test.png", UUID.randomUUID());
	}

	@Nested
	@DisplayName("도메인 이미지 생성")
	class CreateDomainImage {

		@Test
		@DisplayName("유효한 파라미터로 도메인 이미지를 생성한다")
		void create_validParams_createsDomainImage() {
			Image image = createImage();

			DomainImage domainImage = DomainImage.create(DEFAULT_DOMAIN_TYPE, DEFAULT_DOMAIN_ID, image, 1);

			assertThat(domainImage.getDomainType()).isEqualTo(DEFAULT_DOMAIN_TYPE);
			assertThat(domainImage.getDomainId()).isEqualTo(DEFAULT_DOMAIN_ID);
			assertThat(domainImage.getImage()).isEqualTo(image);
			assertThat(domainImage.getSortOrder()).isEqualTo(1);
		}

		@Test
		@DisplayName("정렬 순서가 null이면 0으로 기본값이 설정된다")
		void create_nullSortOrder_defaultsToZero() {
			Image image = createImage();

			DomainImage domainImage = DomainImage.create(DEFAULT_DOMAIN_TYPE, DEFAULT_DOMAIN_ID, image, null);

			assertThat(domainImage.getSortOrder()).isEqualTo(0);
		}

		@Test
		@DisplayName("도메인 타입이 null이면 생성에 실패한다")
		void create_nullDomainType_throwsIllegalArgumentException() {
			Image image = createImage();

			assertThatThrownBy(() -> DomainImage.create(null, DEFAULT_DOMAIN_ID, image, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("도메인 ID가 null이면 생성에 실패한다")
		void create_nullDomainId_throwsIllegalArgumentException() {
			Image image = createImage();

			assertThatThrownBy(() -> DomainImage.create(DEFAULT_DOMAIN_TYPE, null, image, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("도메인 ID가 0이면 생성에 실패한다")
		void create_zeroDomainId_throwsIllegalArgumentException() {
			Image image = createImage();

			assertThatThrownBy(() -> DomainImage.create(DEFAULT_DOMAIN_TYPE, 0L, image, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("도메인 ID가 음수이면 생성에 실패한다")
		void create_negativeDomainId_throwsIllegalArgumentException() {
			Image image = createImage();

			assertThatThrownBy(() -> DomainImage.create(DEFAULT_DOMAIN_TYPE, -1L, image, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("이미지가 null이면 생성에 실패한다")
		void create_nullImage_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> DomainImage.create(DEFAULT_DOMAIN_TYPE, DEFAULT_DOMAIN_ID, null, 0))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("정렬 순서 수정")
	class ChangeSortOrder {

		@Test
		@DisplayName("유효한 정렬 순서로 수정하면 값이 변경된다")
		void changeSortOrder_validOrder_updatesSortOrder() {
			Image image = createImage();
			DomainImage domainImage = DomainImage.create(DEFAULT_DOMAIN_TYPE, DEFAULT_DOMAIN_ID, image, 0);

			domainImage.changeSortOrder(5);

			assertThat(domainImage.getSortOrder()).isEqualTo(5);
		}

		@Test
		@DisplayName("정렬 순서를 null로 수정하면 0으로 기본값이 설정된다")
		void changeSortOrder_nullOrder_defaultsToZero() {
			Image image = createImage();
			DomainImage domainImage = DomainImage.create(DEFAULT_DOMAIN_TYPE, DEFAULT_DOMAIN_ID, image, 3);

			domainImage.changeSortOrder(null);

			assertThat(domainImage.getSortOrder()).isEqualTo(0);
		}
	}
}
