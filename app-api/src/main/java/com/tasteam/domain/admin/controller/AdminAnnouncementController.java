package com.tasteam.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.controller.docs.AdminAnnouncementControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminAnnouncementCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminAnnouncementUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementListItem;
import com.tasteam.domain.admin.service.AdminAnnouncementService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/announcements")
public class AdminAnnouncementController implements AdminAnnouncementControllerDocs {

	private final AdminAnnouncementService adminAnnouncementService;

	@Override
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Page<AdminAnnouncementListItem>> getAnnouncements(
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable) {

		Page<AdminAnnouncementListItem> result = adminAnnouncementService.getAnnouncementList(pageable);
		return SuccessResponse.success(result);
	}

	@Override
	@GetMapping("/{announcementId}")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminAnnouncementDetailResponse> getAnnouncement(
		@PathVariable
		Long announcementId) {

		AdminAnnouncementDetailResponse result = adminAnnouncementService.getAnnouncementDetail(announcementId);
		return SuccessResponse.success(result);
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Long> createAnnouncement(
		@RequestBody @Validated
		AdminAnnouncementCreateRequest request) {

		Long announcementId = adminAnnouncementService.createAnnouncement(request);
		return SuccessResponse.success(announcementId);
	}

	@Override
	@PatchMapping("/{announcementId}")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Void> updateAnnouncement(
		@PathVariable
		Long announcementId,
		@RequestBody @Validated
		AdminAnnouncementUpdateRequest request) {

		adminAnnouncementService.updateAnnouncement(announcementId, request);
		return SuccessResponse.success();
	}

	@Override
	@DeleteMapping("/{announcementId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteAnnouncement(
		@PathVariable
		Long announcementId) {

		adminAnnouncementService.deleteAnnouncement(announcementId);
	}
}
