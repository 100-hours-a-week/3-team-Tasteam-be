package com.tasteam.domain.admin.policy;

import org.springframework.stereotype.Component;

import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.MemberRole;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

@Component
public class AdminAuthPolicy {

	public void validateAdmin(Member member) {
		if (member == null) {
			throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED, "인증되지 않은 사용자입니다");
		}
		if (member.getRole() != MemberRole.ADMIN) {
			throw new BusinessException(CommonErrorCode.NO_PERMISSION, "관리자 권한이 필요합니다");
		}
	}
}
