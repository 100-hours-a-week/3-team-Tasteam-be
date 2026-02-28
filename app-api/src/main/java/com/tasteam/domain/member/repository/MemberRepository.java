package com.tasteam.domain.member.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tasteam.domain.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	Optional<Member> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByIdAndDeletedAtIsNull(Long id);

	Optional<Member> findById(Long id);

	@Query("SELECT m FROM Member m WHERE m.email IS NOT NULL AND m.email <> '' AND m.deletedAt IS NULL")
	Page<Member> findAllWithEmail(Pageable pageable);
}
