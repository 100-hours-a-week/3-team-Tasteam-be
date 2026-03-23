package com.tasteam.global.security.user.repository.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.security.user.dto.UserAccount;
import com.tasteam.global.security.user.repository.UserRepositoryPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepositoryPort {

	private final MemberRepository memberRepository;

	@Override
	public Optional<UserAccount> findByEmail(String email) {
		return memberRepository.findByEmail(email).map(UserAccount::from);
	}

	@Override
	public Optional<UserAccount> findById(Long id) {
		return memberRepository.findById(id).map(UserAccount::from);
	}

	@Override
	public UserAccount save(UserAccount account) {
		Member member = Member.create(account.email(), account.nickname());
		memberRepository.save(member);
		return UserAccount.from(member);
	}

	@Override
	public boolean existsByEmail(String email) {
		return memberRepository.existsByEmail(email);
	}

	@Override
	public void touchLoginSuccess(Long memberId) {
		memberRepository.findById(memberId).ifPresent(member -> {
			member.loginSuccess();
			memberRepository.save(member);
		});
	}
}
