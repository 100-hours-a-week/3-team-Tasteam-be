package com.tasteam.global.security.user.repository;

import java.util.Optional;

import com.tasteam.global.security.user.dto.UserAccount;

public interface UserRepositoryPort {

	Optional<UserAccount> findByEmail(String email);

	Optional<UserAccount> findById(Long id);

	UserAccount save(UserAccount account);

	boolean existsByEmail(String email);

	void touchLoginSuccess(Long memberId);
}
