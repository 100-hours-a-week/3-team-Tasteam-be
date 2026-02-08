package com.tasteam.domain.search.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.search.entity.MemberSearchHistory;

public interface MemberSearchHistoryRepository extends JpaRepository<MemberSearchHistory, Long> {

	Optional<MemberSearchHistory> findByMemberIdAndKeywordAndDeletedAtIsNull(Long memberId, String keyword);

	List<MemberSearchHistory> findAllByMemberIdAndKeywordAndDeletedAtIsNull(Long memberId, String keyword);

	Optional<MemberSearchHistory> findByIdAndMemberIdAndDeletedAtIsNull(Long id, Long memberId);

	@Modifying
	@Query(value = """
		INSERT INTO member_serach_history (member_id, keyword, count, created_at, updated_at, deleted_at)
		VALUES (:memberId, :keyword, 1, NOW(), NOW(), NULL)
		ON CONFLICT (member_id, keyword, deleted_at)
		DO UPDATE SET count = member_serach_history.count + 1, updated_at = NOW()
		""", nativeQuery = true)
	void upsertSearchHistory(@Param("memberId")
	Long memberId, @Param("keyword")
	String keyword);
}
