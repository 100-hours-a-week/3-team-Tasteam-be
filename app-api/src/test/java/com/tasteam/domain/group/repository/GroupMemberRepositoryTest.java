package com.tasteam.domain.group.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.group.entity.GroupMember;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.fixture.MemberFixture;

import jakarta.persistence.EntityManager;

@RepositoryJpaTest
@DisplayName("GroupMemberRepository 테스트")
class GroupMemberRepositoryTest {

	@Autowired
	private GroupMemberRepository groupMemberRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	@DisplayName("그룹 회원을 저장하면 기본 매핑과 연관관계가 정상이다")
	void saveAndFind() {
		Member member = memberRepository.save(MemberFixture.create());
		GroupMember groupMember = GroupMember.create(100L, member);

		GroupMember saved = groupMemberRepository.save(groupMember);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getGroupId()).isEqualTo(100L);
		assertThat(saved.getMember().getId()).isEqualTo(member.getId());
		assertThat(saved.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("findByGroupIdAndMember_IdAndDeletedAtIsNull - 탈퇴한 그룹 회원은 조회되지 않는다")
	void findByGroupIdAndMember_IdAndDeletedAtIsNull_excludesSoftDeleted() {
		Member member = memberRepository.save(MemberFixture.create());
		GroupMember groupMember = groupMemberRepository.save(GroupMember.create(200L, member));
		groupMember.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		var result = groupMemberRepository.findByGroupIdAndMember_IdAndDeletedAtIsNull(200L, member.getId());

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("countByGroupIdAndDeletedAtIsNull - 탈퇴한 회원은 카운트에서 제외된다")
	void countByGroupIdAndDeletedAtIsNull_excludesDeleted() {
		Member member1 = memberRepository.save(MemberFixture.create("active@test.com", "활성회원"));
		Member member2 = memberRepository.save(MemberFixture.create("deleted@test.com", "탈퇴회원"));
		groupMemberRepository.save(GroupMember.create(300L, member1));
		GroupMember deletedMember = groupMemberRepository.save(GroupMember.create(300L, member2));
		deletedMember.softDelete(Instant.now());
		entityManager.flush();
		entityManager.clear();

		long count = groupMemberRepository.countByGroupIdAndDeletedAtIsNull(300L);

		assertThat(count).isEqualTo(1);
	}

	@Test
	@DisplayName("save - 동일한 (group_id, member_id)를 저장하면 제약조건 예외가 발생한다")
	void save_duplicateGroupMember_throwsDataIntegrityViolationException() {
		Member member = memberRepository.save(MemberFixture.create());
		groupMemberRepository.saveAndFlush(GroupMember.create(400L, member));

		assertThatThrownBy(() -> groupMemberRepository.saveAndFlush(GroupMember.create(400L, member)))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
