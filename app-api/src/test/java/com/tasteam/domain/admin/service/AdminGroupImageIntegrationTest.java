package com.tasteam.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.admin.dto.response.AdminGroupListItem;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.entity.Image;
import com.tasteam.domain.file.entity.ImageStatus;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.restaurant.dto.GeocodingResult;
import com.tasteam.domain.restaurant.geocoding.NaverGeocodingClient;
import com.tasteam.fixture.AdminGroupRequestFixture;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.FileErrorCode;

@ServiceIntegrationTest
@Transactional
class AdminGroupImageIntegrationTest {

	private static final String MISSING_FILE_UUID = "11111111-2222-3333-4444-555555555555";

	@Autowired
	private AdminGroupService adminGroupService;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	@MockitoBean
	private NaverGeocodingClient naverGeocodingClient;

	@BeforeEach
	void setUp() {
		given(naverGeocodingClient.geocode(anyString())).willReturn(
			new GeocodingResult("서울특별시", "강남구", "역삼동", "06234", 127.0365, 37.4979));
	}

	@Nested
	@DisplayName("관리자 그룹 생성 시 이미지 처리")
	class CreateGroupWithImage {

		@Test
		@DisplayName("로고 이미지와 함께 그룹을 생성하면 이미지가 ACTIVE로 전환된다")
		void createGroup_withLogoImage_activatesImage() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "uploads/group/image/admin-logo.png",
				fileUuid, "admin-logo.png"));

			var request = AdminGroupRequestFixture.createRequest(fileUuid.toString());

			Long groupId = adminGroupService.createGroup(request);

			Image updatedImage = imageRepository.findByFileUuid(fileUuid).orElseThrow();
			assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.GROUP, List.of(groupId));
			assertThat(links).hasSize(1);
		}

		@Test
		@DisplayName("로고 이미지 없이 그룹을 생성하면 DomainImage가 생성되지 않는다")
		void createGroup_withoutLogoImage_noDomainImage() {
			var request = AdminGroupRequestFixture.createRequest();

			Long groupId = adminGroupService.createGroup(request);

			List<DomainImage> links = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
				DomainType.GROUP, List.of(groupId));
			assertThat(links).isEmpty();
		}

		@Test
		@DisplayName("존재하지 않는 로고 이미지를 지정하면 그룹 생성에 실패한다")
		void createGroup_withMissingLogoImage_fails() {
			var request = AdminGroupRequestFixture.createRequest(MISSING_FILE_UUID);

			assertThatThrownBy(() -> adminGroupService.createGroup(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(FileErrorCode.FILE_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("관리자 그룹 목록 조회 시 로고 이미지 해석")
	class GetGroupsWithImage {

		@Test
		@DisplayName("로고 이미지가 있는 그룹은 목록에서 logoImageUrl이 반환된다")
		void getGroups_withLogoImage_returnsLogoUrl() {
			UUID fileUuid = UUID.randomUUID();
			imageRepository.save(ImageFixture.create(FilePurpose.GROUP_IMAGE, "uploads/group/image/list-logo.png",
				fileUuid, "list-logo.png"));

			var request = AdminGroupRequestFixture.createRequest(fileUuid.toString());

			adminGroupService.createGroup(request);

			Page<AdminGroupListItem> page = adminGroupService.getGroups(PageRequest.of(0, 10));

			assertThat(page.getContent()).hasSize(1);
			assertThat(page.getContent().getFirst().logoImageUrl()).isNotNull();
			assertThat(page.getContent().getFirst().logoImageUrl()).contains("list-logo.png");
		}

		@Test
		@DisplayName("로고 이미지가 없는 그룹은 목록에서 logoImageUrl이 null이다")
		void getGroups_withoutLogoImage_returnsLogoUrlNull() {
			var request = AdminGroupRequestFixture.createRequest();

			adminGroupService.createGroup(request);

			Page<AdminGroupListItem> page = adminGroupService.getGroups(PageRequest.of(0, 10));

			assertThat(page.getContent()).hasSize(1);
			assertThat(page.getContent().getFirst().logoImageUrl()).isNull();
		}
	}
}
