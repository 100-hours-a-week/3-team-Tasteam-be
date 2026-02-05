package com.tasteam.domain.admin.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.MemberRole;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.exception.business.BusinessException;

@UnitTest
@DisplayName("관리자 권한 정책")
class AdminAuthPolicyTest {

	private final AdminAuthPolicy policy = new AdminAuthPolicy();

	@Nested
	@DisplayName("관리자 권한 검증")
	class ValidateAdmin {

		@Test
		@DisplayName("회원이 null이면 BusinessException을 발생시킨다")
		void validateAdmin_nullMember_throwsBusinessException() {
			assertThatThrownBy(() -> policy.validateAdmin(null))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("회원의 역할이 USER이면 BusinessException을 발생시킨다")
		void validateAdmin_userRole_throwsBusinessException() {
			Member member = MemberFixture.create();

			assertThatThrownBy(() -> policy.validateAdmin(member))
				.isInstanceOf(BusinessException.class);
		}

		@Test
		@DisplayName("회원의 역할이 ADMIN이면 예외를 발생시키지 않는다")
		void validateAdmin_adminRole_doesNotThrow() {
			Member member = MemberFixture.create();
			setRole(member, MemberRole.ADMIN);

			assertThatCode(() -> policy.validateAdmin(member))
				.doesNotThrowAnyException();
		}
	}

	private static void setRole(Member member, MemberRole role) {
		try {
			Field roleField = Member.class.getDeclaredField("role");
			roleField.setAccessible(true);
			roleField.set(member, role);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Failed to set member role", e);
		}
	}
}
