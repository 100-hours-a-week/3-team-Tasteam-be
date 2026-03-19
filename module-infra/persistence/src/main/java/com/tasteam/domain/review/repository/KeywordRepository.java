package com.tasteam.domain.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.review.entity.Keyword;
import com.tasteam.domain.review.entity.KeywordType;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

	List<Keyword> findAllByOrderByIdAsc();

	List<Keyword> findByTypeOrderByIdAsc(KeywordType type);
}
