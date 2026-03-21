package com.tasteam.domain.announcement.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.announcement.controller.docs.AnnouncementControllerDocs;
import com.tasteam.domain.announcement.dto.response.AnnouncementDetailResponse;
import com.tasteam.domain.announcement.dto.response.AnnouncementListResponse;
import com.tasteam.domain.announcement.service.AnnouncementService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController implements AnnouncementControllerDocs {

	private final AnnouncementService announcementService;

	@Override
	@GetMapping
	public SuccessResponse<OffsetPageResponse<AnnouncementListResponse>> getAnnouncementList(
		@PageableDefault(size = 20)
		Pageable pageable) {
		return SuccessResponse.success(announcementService.getAnnouncementList(pageable));
	}

	@Override
	@GetMapping("/{announcementId}")
	public SuccessResponse<AnnouncementDetailResponse> getAnnouncementDetail(@PathVariable
	Long announcementId) {
		return SuccessResponse.success(announcementService.getAnnouncementDetail(announcementId));
	}
}
