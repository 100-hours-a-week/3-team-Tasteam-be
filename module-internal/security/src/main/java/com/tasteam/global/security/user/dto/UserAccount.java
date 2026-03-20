package com.tasteam.global.security.user.dto;

import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.MemberRole;

/**
 * 애플리케이션-레벨에서 사용하는 회원 정보 DTO.
 * <p>Member 엔티티 변경 영향범위를 축소하기 위해 Member 도메인 의존을 이 레이어에서만 캡슐화합니다.</p>
 *
 * @param id 등록된 사용자 ID
 * @param email 이메일
 * @param password 암호화된 비밀번호
 * @param nickname 닉네임
 * @param profileImageUrl 프로필 이미지 URL
 * @param role 회원 역할(enum)
 * @param active 활성화 여부
 */
public record UserAccount(
	Long id,
	String email,
	String password,
	String nickname,
	String profileImageUrl,
	MemberRole role,
	boolean active) {

	public static UserAccount from(Member member) {
		return new UserAccount(
			member.getId(),
			member.getEmail(),
			null,
			member.getNickname(),
			member.getProfileImageUrl(),
			member.getRole(),
			member.isActive());
	}
}
