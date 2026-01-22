package com.tasteam.domain.search.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.search.entity.MemberSearchHistory;

public interface MemberSearchHistoryRepository extends JpaRepository<MemberSearchHistory, Long> {

	Optional<MemberSearchHistory> findByMemberIdAndKeywordAndDeletedAtIsNull(Long memberId, String keyword);

	Optional<MemberSearchHistory> findByIdAndMemberIdAndDeletedAtIsNull(Long id, Long memberId);

	@Query("""
		select msh
		from MemberSearchHistory msh
		where msh.memberId = :memberId
		  and msh.deletedAt is null
		  and (
		    :cursorUpdatedAt is null
		    or msh.updatedAt < :cursorUpdatedAt
		    or (msh.updatedAt = :cursorUpdatedAt and msh.id < :cursorId)
		  )
		order by msh.updatedAt desc, msh.id desc
		""")
	List<MemberSearchHistory> findRecentSearches(
		@Param("memberId")
		Long memberId,
		@Param("cursorUpdatedAt")
		Instant cursorUpdatedAt,
		@Param("cursorId")
		Long cursorId,
		Pageable pageable);
}
