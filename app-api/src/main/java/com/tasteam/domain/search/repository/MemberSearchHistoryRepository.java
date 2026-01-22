package com.tasteam.domain.search.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.search.entity.MemberSearchHistory;

public interface MemberSearchHistoryRepository extends JpaRepository<MemberSearchHistory, Long> {

	Optional<MemberSearchHistory> findByMemberIdAndKeywordAndDeletedAtIsNull(Long memberId, String keyword);
}
