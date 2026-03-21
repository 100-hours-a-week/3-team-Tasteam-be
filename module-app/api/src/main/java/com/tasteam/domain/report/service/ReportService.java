package com.tasteam.domain.report.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.report.dto.request.ReportCreateRequest;
import com.tasteam.domain.report.dto.response.ReportCreateResponse;
import com.tasteam.domain.report.dto.response.ReportListItem;
import com.tasteam.domain.report.entity.Report;
import com.tasteam.domain.report.repository.ReportRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.MemberErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportService {

	private final ReportRepository reportRepository;
	private final MemberRepository memberRepository;

	@Transactional
	public ReportCreateResponse submit(Long memberId, ReportCreateRequest request) {
		Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		Report report = Report.create(member, request.category(), request.content());
		reportRepository.save(report);

		return new ReportCreateResponse(report.getId(), report.getCreatedAt());
	}

	@Transactional(readOnly = true)
	public Page<ReportListItem> getMyReports(Long memberId, Pageable pageable) {
		return reportRepository.findByMember_Id(memberId, pageable)
			.map(r -> new ReportListItem(
				r.getId(),
				r.getCategory(),
				r.getContent(),
				r.getStatus(),
				r.getCreatedAt()));
	}
}
