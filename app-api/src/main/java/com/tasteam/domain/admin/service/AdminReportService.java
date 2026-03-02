package com.tasteam.domain.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.response.AdminReportDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminReportListItem;
import com.tasteam.domain.report.entity.Report;
import com.tasteam.domain.report.entity.ReportCategory;
import com.tasteam.domain.report.entity.ReportStatus;
import com.tasteam.domain.report.repository.ReportRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.ReportErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminReportService {

	private static final int CONTENT_PREVIEW_LENGTH = 60;

	private final ReportRepository reportRepository;

	@Transactional(readOnly = true)
	public Page<AdminReportListItem> getReports(ReportCategory category, ReportStatus status, Pageable pageable) {
		Page<Report> reports;

		if (category != null && status != null) {
			reports = reportRepository.findByCategoryAndStatus(category, status, pageable);
		} else if (category != null) {
			reports = reportRepository.findByCategory(category, pageable);
		} else if (status != null) {
			reports = reportRepository.findByStatus(status, pageable);
		} else {
			reports = reportRepository.findAll(pageable);
		}

		return reports.map(r -> new AdminReportListItem(
			r.getId(),
			r.getMember().getNickname(),
			r.getCategory(),
			truncate(r.getContent()),
			r.getStatus(),
			r.getCreatedAt()));
	}

	@Transactional(readOnly = true)
	public AdminReportDetailResponse getDetail(Long reportId) {
		Report report = reportRepository.findById(reportId)
			.orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));

		return new AdminReportDetailResponse(
			report.getId(),
			report.getMember().getId(),
			report.getMember().getNickname(),
			report.getMember().getEmail(),
			report.getCategory(),
			report.getContent(),
			report.getStatus(),
			report.getCreatedAt(),
			report.getUpdatedAt());
	}

	@Transactional
	public void updateStatus(Long reportId, ReportStatus status) {
		Report report = reportRepository.findById(reportId)
			.orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));

		report.updateStatus(status);
	}

	private String truncate(String text) {
		if (text == null) {
			return null;
		}
		return text.length() > CONTENT_PREVIEW_LENGTH
			? text.substring(0, CONTENT_PREVIEW_LENGTH) + "..."
			: text;
	}
}
