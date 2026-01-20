package com.tasteam.domain.member.entity.oauth;

import com.tasteam.domain.common.BaseCreatedAtEntity;
import com.tasteam.domain.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "member_oauth_account", uniqueConstraints = {
	@UniqueConstraint(name = "uk_member_oauth_provider_user", columnNames = {"provider", "provider_user_id"})
})
public class MemberOAuthAccount extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "provider", nullable = false, length = 20)
	private String provider;

	@Column(name = "provider_user_id", nullable = false, length = 100)
	private String providerUserId;

	@Column(name = "provider_user_email", length = 255)
	private String providerUserEmail;

	public static MemberOAuthAccount create(String provider, String providerUserId, String providerUserEmail,
			Member member) {
		return MemberOAuthAccount.builder()
			.provider(provider)
			.providerUserId(providerUserId)
			.providerUserEmail(providerUserEmail)
			.member(member)
			.build();
	}
}
