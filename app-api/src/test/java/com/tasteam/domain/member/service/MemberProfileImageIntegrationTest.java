package com.tasteam.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.file.entity.DomainImage;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.fixture.ImageFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.fixture.MemberRequestFixture;

@ServiceIntegrationTest
@Transactional
class MemberProfileImageIntegrationTest {

	@Autowired
	private MemberService memberService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ImageRepository imageRepository;

	@Autowired
	private DomainImageRepository domainImageRepository;

	@Test
	@DisplayName("프로필 이미지 업데이트 후 조회 시 이미지가 정상적으로 반환되어야 한다")
	void updateProfileImage_thenFindByDomainTypeAndDomainId_shouldReturnImage() {
		Member member = memberRepository.save(MemberFixture.create());
		UUID fileUuid = UUID.randomUUID();
		imageRepository.save(
			ImageFixture.create(FilePurpose.PROFILE_IMAGE, "members/" + member.getId() + "/profile.png", fileUuid));

		var request = MemberRequestFixture.profileUpdateRequest(null, null, null, fileUuid.toString());

		memberService.updateMyProfile(member.getId(), request);

		List<DomainImage> result = domainImageRepository.findAllByDomainTypeAndDomainIdIn(
			DomainType.MEMBER, List.of(member.getId()));

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getImage().getFileUuid()).isEqualTo(fileUuid);
	}

	@Test
	@DisplayName("프로필 이미지 업데이트 후 getMyProfile 조회 시 이미지 URL이 반환되어야 한다")
	void updateProfileImage_thenGetMyProfile_shouldReturnImageUrl() {
		Member member = memberRepository.save(MemberFixture.create());
		UUID fileUuid = UUID.randomUUID();
		imageRepository.save(
			ImageFixture.create(FilePurpose.PROFILE_IMAGE, "members/" + member.getId() + "/profile.png", fileUuid));

		var request = MemberRequestFixture.profileUpdateRequest(null, null, null, fileUuid.toString());

		memberService.updateMyProfile(member.getId(), request);

		var response = memberService.getMyProfile(member.getId());

		assertThat(response.member().profileImageUrl()).isNotNull();
		assertThat(response.member().profileImageUrl()).contains("profile.png");
	}
}
