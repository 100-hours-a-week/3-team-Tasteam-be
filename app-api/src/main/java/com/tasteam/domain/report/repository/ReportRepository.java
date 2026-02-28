package com.tasteam.domain.report.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.report.entity.Report;
import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;

public interface ReportRepository extends JpaRepository<Report, Long> {

	Page<Report> findByMember_Id(Long memberId, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"member"})
	Page<Report> findAll(Pageable pageable);

	@EntityGraph(attributePaths = {"member"})
	Page<Report> findByCategory(ReportCategory category, Pageable pageable);

	@EntityGraph(attributePaths = {"member"})
	Page<Report> findByStatus(ReportStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"member"})
	Page<Report> findByCategoryAndStatus(ReportCategory category, ReportStatus status, Pageable pageable);
}
