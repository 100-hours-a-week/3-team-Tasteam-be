package com.tasteam.domain.subgroup.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.subgroup.entity.SubgroupMember;
import com.tasteam.fixture.MemberFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("SubgroupMemberRepository 테스트")
class SubgroupMemberRepositoryTest {

	@Autowired
	private SubgroupMemberRepository subgroupMemberRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("하위그룹 회원을 저장하면 기본 매핑과 연관관계가 정상이다")
	void saveAndFind() {
		Member member = memberRepository.save(MemberFixture.create());
		SubgroupMember subgroupMember = SubgroupMember.create(100L, member);

		SubgroupMember saved = subgroupMemberRepository.save(subgroupMember);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getSubgroupId()).isEqualTo(100L);
		assertThat(saved.getMember().getId()).isEqualTo(member.getId());
		assertThat(saved.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("findBySubgroupIdAndMember_IdAndDeletedAtIsNull - 탈퇴한 하위그룹 회원은 조회되지 않는다")
	void findBySubgroupIdAndMember_IdAndDeletedAtIsNull_excludesSoftDeleted() {
		Member member = memberRepository.save(MemberFixture.create());
		SubgroupMember subgroupMember = subgroupMemberRepository.save(SubgroupMember.create(200L, member));
		subgroupMember.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		var result = subgroupMemberRepository.findBySubgroupIdAndMember_IdAndDeletedAtIsNull(200L, member.getId());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("findBySubgroupIdAndMember_Id - deletedAt 필터 없이 탈퇴한 회원도 포함된다")
	void findBySubgroupIdAndMember_Id_includesDeleted() {
		Member member = memberRepository.save(MemberFixture.create());
		SubgroupMember subgroupMember = subgroupMemberRepository.save(SubgroupMember.create(300L, member));
		subgroupMember.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		var result = subgroupMemberRepository.findBySubgroupIdAndMember_Id(300L, member.getId());

		assertThat(result).isPresent();
		assertThat(result.get().getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("save - 동일한 (subgroup_id, member_id)를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateSubgroupMember_throwsDataIntegrityViolationException() {
		Member member = memberRepository.save(MemberFixture.create());
		subgroupMemberRepository.save(SubgroupMember.create(400L, member));
		assertThatThrownBy(() -> subgroupMemberRepository.saveAndFlush(SubgroupMember.create(400L, member)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
