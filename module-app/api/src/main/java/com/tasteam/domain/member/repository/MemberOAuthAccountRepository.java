package com.tasteam.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;

public interface MemberOAuthAccountRepository extends JpaRepository<MemberOAuthAccount, Long> {
	Optional<MemberOAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
}
