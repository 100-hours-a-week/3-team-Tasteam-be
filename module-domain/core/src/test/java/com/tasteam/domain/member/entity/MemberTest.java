package com.tasteam.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Member) Member 단위 테스트")
class MemberTest {

	@Nested
	@DisplayName("회원 생성")
	class CreateMember {

		@Test
		@DisplayName("유효한 이메일과 닉네임으로 회원을 생성하면 ACTIVE 상태의 USER 역할로 생성된다")
		void create_validEmailAndNickname_createsActiveMemberWithUserRole() {
			Member member = Member.create("test@example.com", "테스트유저");

			assertThat(member.getEmail()).isEqualTo("test@example.com");
			assertThat(member.getNickname()).isEqualTo("테스트유저");
			assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
			assertThat(member.getRole()).isEqualTo(MemberRole.USER);
		}

		@Test
		@DisplayName("이메일이 null이면 검증을 건너뛰고 회원을 생성한다")
		void create_nullEmail_createsActiveMember() {
			Member member = Member.create(null, "테스트유저");

			assertThat(member.getEmail()).isNull();
			assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
		}

		@Test
		@DisplayName("이메일이 빈 문자열이면 회원 생성에 실패한다")
		void create_blankEmail_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Member.create("  ", "테스트유저"))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("닉네임이 빈 문자열이면 회원 생성에 실패한다")
		void create_blankNickname_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Member.create("test@example.com", "  "))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("닉네임이 최대 길이를 초과하면 회원 생성에 실패한다")
		void create_nicknameTooLong_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Member.create("test@example.com", "a".repeat(31)))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("회원 정보 수정")
	class UpdateMember {

		@Test
		@DisplayName("유효한 닉네임으로 수정하면 닉네임이 변경된다")
		void changeNickname_validNickname_updatesNickname() {
			Member member = Member.create("test@example.com", "원래닉네임");

			member.changeNickname("새닉네임");

			assertThat(member.getNickname()).isEqualTo("새닉네임");
		}

		@Test
		@DisplayName("닉네임을 빈 문자열로 수정하면 실패한다")
		void changeNickname_blankNickname_throwsIllegalArgumentException() {
			Member member = Member.create("test@example.com", "테스트유저");

			assertThatThrownBy(() -> member.changeNickname(""))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("닉네임이 최대 길이를 초과하면 수정에 실패한다")
		void changeNickname_tooLong_throwsIllegalArgumentException() {
			Member member = Member.create("test@example.com", "테스트유저");

			assertThatThrownBy(() -> member.changeNickname("a".repeat(31)))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("유효한 이메일로 수정하면 이메일이 변경된다")
		void changeEmail_validEmail_updatesEmail() {
			Member member = Member.create("old@example.com", "테스트유저");

			member.changeEmail("new@example.com");

			assertThat(member.getEmail()).isEqualTo("new@example.com");
		}

		@Test
		@DisplayName("이메일을 null로 수정하면 null로 변경된다")
		void changeEmail_nullEmail_updatesToNull() {
			Member member = Member.create("old@example.com", "테스트유저");

			member.changeEmail(null);

			assertThat(member.getEmail()).isNull();
		}

		@Test
		@DisplayName("이메일을 빈 문자열로 수정하면 실패한다")
		void changeEmail_blankEmail_throwsIllegalArgumentException() {
			Member member = Member.create("old@example.com", "테스트유저");

			assertThatThrownBy(() -> member.changeEmail(""))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("이메일이 최대 길이를 초과하면 수정에 실패한다")
		void changeEmail_tooLong_throwsIllegalArgumentException() {
			Member member = Member.create("old@example.com", "테스트유저");

			assertThatThrownBy(() -> member.changeEmail("a".repeat(256)))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("프로필 이미지 URL을 null로 수정하면 null로 변경된다")
		void changeProfileImageUrl_nullUrl_updatesToNull() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.changeProfileImageUrl(null);

			assertThat(member.getProfileImageUrl()).isNull();
		}

		@Test
		@DisplayName("프로필 이미지 URL을 빈 문자열로 수정하면 실패한다")
		void changeProfileImageUrl_blankUrl_throwsIllegalArgumentException() {
			Member member = Member.create("test@example.com", "테스트유저");

			assertThatThrownBy(() -> member.changeProfileImageUrl("  "))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("유효한 프로필 이미지 URL로 수정하면 URL이 변경된다")
		void changeProfileImageUrl_validUrl_updatesUrl() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.changeProfileImageUrl("https://example.com/new-profile.jpg");

			assertThat(member.getProfileImageUrl()).isEqualTo("https://example.com/new-profile.jpg");
		}

		@Test
		@DisplayName("자기소개를 null로 수정하면 null로 변경된다")
		void changeIntroduction_nullIntro_updatesToNull() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.changeIntroduction(null);

			assertThat(member.getIntroduction()).isNull();
		}

		@Test
		@DisplayName("자기소개가 최대 길이를 초과하면 수정에 실패한다")
		void changeIntroduction_tooLong_throwsIllegalArgumentException() {
			Member member = Member.create("test@example.com", "테스트유저");

			assertThatThrownBy(() -> member.changeIntroduction("a".repeat(501)))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("회원 상태 전이")
	class MemberStatusTransition {

		@Test
		@DisplayName("회원을 차단하면 상태가 BLOCKED로 변경된다")
		void block_changesStatusToBlocked() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.block();

			assertThat(member.getStatus()).isEqualTo(com.tasteam.domain.member.entity.MemberStatus.BLOCKED);
		}

		@Test
		@DisplayName("회원이 탈퇴하면 상태가 WITHDRAWN이고 deletedAt이 설정된다")
		void withdraw_changesStatusToWithdrawnAndSetsDeletedAt() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.withdraw();

			assertThat(member.getStatus()).isEqualTo(com.tasteam.domain.member.entity.MemberStatus.WITHDRAWN);
			assertThat(member.getDeletedAt()).isNotNull();
		}

		@Test
		@DisplayName("탈퇴한 회원을 복구하면 상태가 ACTIVE이고 deletedAt이 초기화된다")
		void activate_changesStatusToActiveAndClearsDeletedAt() {
			Member member = Member.create("test@example.com", "테스트유저");
			member.withdraw();

			member.activate();

			assertThat(member.getStatus()).isEqualTo(com.tasteam.domain.member.entity.MemberStatus.ACTIVE);
			assertThat(member.getDeletedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("회원 활동 기록")
	class MemberActivity {

		@Test
		@DisplayName("로그인 성공 시 lastLoginAt이 설정된다")
		void loginSuccess_setsLastLoginAt() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.loginSuccess();

			assertThat(member.getLastLoginAt()).isNotNull();
		}

		@Test
		@DisplayName("약관 동의 시 agreedTermsAt이 설정된다")
		void agreeTerms_setsAgreedTermsAt() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.agreeTerms();

			assertThat(member.getAgreedTermsAt()).isNotNull();
		}

		@Test
		@DisplayName("개인정보 동의 시 agreedPrivacyAt이 설정된다")
		void agreePrivacy_setsAgreedPrivacyAt() {
			Member member = Member.create("test@example.com", "테스트유저");

			member.agreePrivacy();

			assertThat(member.getAgreedPrivacyAt()).isNotNull();
		}

		@Test
		@DisplayName("ACTIVE 상태의 회원은 isActive가 true를 반환한다")
		void isActive_returnsTrue_whenStatusIsActive() {
			Member member = Member.create("test@example.com", "테스트유저");

			assertThat(member.isActive()).isTrue();
		}

		@Test
		@DisplayName("차단된 회원은 isActive가 false를 반환한다")
		void isActive_returnsFalse_whenStatusIsBlocked() {
			Member member = Member.create("test@example.com", "테스트유저");
			member.block();

			assertThat(member.isActive()).isFalse();
		}
	}
}
