package com.tasteam.domain.admin.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.admin.dto.request.AdminAnnouncementCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminAnnouncementUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminAnnouncementListItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(130)
@Tag(name = "Admin - Announcement", description = "어드민 공지사항 관리 API")
public interface AdminAnnouncementControllerDocs {

	@Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 최신순으로 조회합니다.")
	SuccessResponse<Page<AdminAnnouncementListItem>> getAnnouncements(Pageable pageable);

	@Operation(summary = "공지사항 상세 조회", description = "공지사항 ID로 상세 정보를 조회합니다.")
	SuccessResponse<AdminAnnouncementDetailResponse> getAnnouncement(
		@Parameter(description = "공지사항 ID", example = "1") @PathVariable
		Long announcementId);

	@Operation(summary = "공지사항 등록", description = "새 공지사항을 등록합니다.")
	SuccessResponse<Long> createAnnouncement(
		@RequestBody @Validated
		AdminAnnouncementCreateRequest request);

	@Operation(summary = "공지사항 수정", description = "공지사항 내용을 수정합니다.")
	SuccessResponse<Void> updateAnnouncement(
		@Parameter(description = "공지사항 ID", example = "1") @PathVariable
		Long announcementId,
		@RequestBody @Validated
		AdminAnnouncementUpdateRequest request);

	@Operation(summary = "공지사항 삭제", description = "공지사항을 삭제합니다.")
	void deleteAnnouncement(
		@Parameter(description = "공지사항 ID", example = "1") @PathVariable
		Long announcementId);
}
