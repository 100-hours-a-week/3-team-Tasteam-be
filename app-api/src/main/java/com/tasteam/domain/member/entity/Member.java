package com.tasteam.domain.member.entity;

import java.time.Instant;

import org.springframework.util.Assert;

import com.tasteam.domain.common.BaseTimeEntity;
import com.tasteam.global.validation.ValidationPatterns;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq_gen")
	@SequenceGenerator(name = "member_seq_gen", sequenceName = "member_id_seq", allocationSize = 1)
	private Long id;

	@Column(name = "email", unique = true, length = 255)
	private String email;

	@Column(name = "nickname", nullable = false, length = 50)
	private String nickname;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private MemberStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private MemberRole role;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	@Column(name = "introduction", length = 500)
	private String introduction;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "agreed_terms_at")
	private Instant agreedTermsAt;

	@Column(name = "agreed_privacy_at")
	private Instant agreedPrivacyAt;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static Member create(String email, String nickname) {
		validateCreate(email, nickname);
		return Member.builder()
			.email(email)
			.nickname(nickname)
			.status(MemberStatus.ACTIVE)
			.role(MemberRole.USER)
			.build();
	}

	public void changeNickname(String nickname) {
		validateNickname(nickname);
		this.nickname = nickname;
	}

	public void changeEmail(String email) {
		validateEmail(email);
		this.email = email;
	}

	public void changeProfileImageUrl(String profileImageUrl) {
		validateProfileImageUrl(profileImageUrl);
		this.profileImageUrl = profileImageUrl;
	}

	public void changeIntroduction(String introduction) {
		validateIntroduction(introduction);
		this.introduction = introduction;
	}

	public void block() {
		this.status = MemberStatus.BLOCKED;
	}

	public void withdraw() {
		this.status = MemberStatus.WITHDRAWN;
		this.deletedAt = Instant.now();
	}

	public void activate() {
		this.status = MemberStatus.ACTIVE;
		this.deletedAt = null;
	}

	public void loginSuccess() {
		this.lastLoginAt = Instant.now();
	}

	public void agreeTerms() {
		this.agreedTermsAt = Instant.now();
	}

	public void agreePrivacy() {
		this.agreedPrivacyAt = Instant.now();
	}

	public boolean isActive() {
		return this.status == MemberStatus.ACTIVE;
	}

	private static void validateCreate(String email, String nickname) {
		validateEmail(email);
		validateNickname(nickname);
	}

	private static void validateEmail(String email) {
		if (email == null) {
			return;
		}
		Assert.hasText(email, "이메일은 필수입니다");
		if (email.length() > 255) {
			throw new IllegalArgumentException("이메일이 너무 깁니다");
		}
	}

	private static void validateNickname(String nickname) {
		Assert.hasText(nickname, "닉네임은 필수입니다");
		if (nickname.length() > ValidationPatterns.NICKNAME_MAX_LENGTH) {
			throw new IllegalArgumentException("닉네임이 너무 깁니다");
		}
	}

	private static void validateProfileImageUrl(String profileImageUrl) {
		if (profileImageUrl == null) {
			return;
		}
		Assert.hasText(profileImageUrl, "프로필 이미지 URL은 필수입니다");
		if (profileImageUrl.length() > 500) {
			throw new IllegalArgumentException("프로필 이미지 URL이 너무 깁니다");
		}
	}

	private static void validateIntroduction(String introduction) {
		if (introduction == null) {
			return;
		}
		if (introduction.length() > ValidationPatterns.INTRODUCTION_MAX_LENGTH) {
			throw new IllegalArgumentException("자기소개가 너무 깁니다");
		}
	}
}
