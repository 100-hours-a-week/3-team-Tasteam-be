package com.tasteam.domain.announcement.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.announcement.dto.response.AnnouncementDetailResponse;
import com.tasteam.domain.announcement.dto.response.AnnouncementListResponse;
import com.tasteam.domain.announcement.entity.Announcement;
import com.tasteam.domain.announcement.repository.AnnouncementRepository;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.PromotionErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

	private final AnnouncementRepository announcementRepository;

	@Transactional(readOnly = true)
	public OffsetPageResponse<AnnouncementListResponse> getAnnouncementList(Pageable pageable) {
		Page<AnnouncementListResponse> result = announcementRepository
			.findAllByDeletedAtIsNullOrderByIdDesc(pageable)
			.map(AnnouncementListResponse::of);

		return new OffsetPageResponse<>(
			result.getContent(),
			new OffsetPagination(
				result.getNumber(),
				result.getSize(),
				result.getTotalPages(),
				(int)result.getTotalElements()));
	}

	@Transactional(readOnly = true)
	public AnnouncementDetailResponse getAnnouncementDetail(Long announcementId) {
		Announcement notice = announcementRepository
			.findByIdAndDeletedAtIsNull(announcementId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND));

		return AnnouncementDetailResponse.of(notice);
	}
}
