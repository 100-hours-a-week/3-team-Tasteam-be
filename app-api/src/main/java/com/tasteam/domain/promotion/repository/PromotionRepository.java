package com.tasteam.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.promotion.entity.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long>, PromotionQueryRepository {

	Optional<Promotion> findByIdAndDeletedAtIsNull(Long id);

	@Query("""
		SELECT p FROM Promotion p
		WHERE p.id = :promotionId
		  AND p.deletedAt IS NULL
		""")
	Optional<Promotion> findByIdWithDisplay(@Param("promotionId")
	Long promotionId);
}
