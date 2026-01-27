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

	@Query(value = """
		select *
		from member_serach_history msh
		where msh.member_id = :memberId
		  and msh.deleted_at is null
		  and (
		    cast(:cursorUpdatedAt as timestamptz) is null
		    or msh.updated_at < :cursorUpdatedAt
		    or (msh.updated_at = :cursorUpdatedAt and msh.id < :cursorId)
		  )
		order by msh.updated_at desc, msh.id desc
		""", nativeQuery = true)
	List<MemberSearchHistory> findRecentSearches(
		@Param("memberId")
		Long memberId,
		@Param("cursorUpdatedAt")
		Instant cursorUpdatedAt,
		@Param("cursorId")
		Long cursorId,
		Pageable pageable);
}
