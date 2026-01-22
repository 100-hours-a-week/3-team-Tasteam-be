package com.tasteam.domain.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.review.entity.Keyword;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {}
