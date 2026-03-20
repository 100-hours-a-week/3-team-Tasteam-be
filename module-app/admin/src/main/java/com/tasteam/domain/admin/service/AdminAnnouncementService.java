package com.tasteam.domain.admin.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.admin.dto.request.AdminAnnouncementCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminAnnouncementUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementListItem;
import com.tasteam.domain.announcement.entity.Announcement;
import com.tasteam.domain.announcement.repository.AnnouncementRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.PromotionErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAnnouncementService {

	private final AnnouncementRepository announcementRepository;

	@Transactional
	public Long createAnnouncement(AdminAnnouncementCreateRequest request) {
		Announcement announcement = Announcement.create(request.title(), request.content());
		announcementRepository.save(announcement);
		return announcement.getId();
	}

	@Transactional(readOnly = true)
	public Page<AdminAnnouncementListItem> getAnnouncementList(Pageable pageable) {
		return announcementRepository.findAllByDeletedAtIsNullOrderByIdDesc(pageable)
			.map(announcement -> new AdminAnnouncementListItem(
				announcement.getId(),
				announcement.getTitle(),
				announcement.getCreatedAt(),
				announcement.getUpdatedAt()));
	}

	@Transactional(readOnly = true)
	public AdminAnnouncementDetailResponse getAnnouncementDetail(Long announcementId) {
		Announcement announcement = announcementRepository.findByIdAndDeletedAtIsNull(announcementId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND));

		return new AdminAnnouncementDetailResponse(
			announcement.getId(),
			announcement.getTitle(),
			announcement.getContent(),
			announcement.getCreatedAt(),
			announcement.getUpdatedAt());
	}

	@Transactional
	public void updateAnnouncement(Long announcementId, AdminAnnouncementUpdateRequest request) {
		Announcement announcement = announcementRepository.findByIdAndDeletedAtIsNull(announcementId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND));

		announcement.update(request.title(), request.content());
	}

	@Transactional
	public void deleteAnnouncement(Long announcementId) {
		Announcement announcement = announcementRepository.findByIdAndDeletedAtIsNull(announcementId)
			.orElseThrow(() -> new BusinessException(PromotionErrorCode.ANNOUNCEMENT_NOT_FOUND));

		announcement.delete(Instant.now());
	}
}
