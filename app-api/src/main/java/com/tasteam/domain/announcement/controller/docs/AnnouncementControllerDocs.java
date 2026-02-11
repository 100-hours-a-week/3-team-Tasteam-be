package com.tasteam.domain.announcement.controller.docs;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.announcement.dto.response.AnnouncementDetailResponse;
import com.tasteam.domain.announcement.dto.response.AnnouncementListResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Announcement", description = "공지사항 API")
public interface AnnouncementControllerDocs {

	@Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 최신순으로 조회합니다.")
	SuccessResponse<OffsetPageResponse<AnnouncementListResponse>> getAnnouncementList(
		@PageableDefault(size = 20) @Parameter(hidden = true)
		Pageable pageable);

	@Operation(summary = "공지사항 상세 조회", description = "특정 공지사항의 상세 정보를 조회합니다.")
	SuccessResponse<AnnouncementDetailResponse> getAnnouncementDetail(
		@Parameter(description = "공지 ID", example = "1") @PathVariable
		Long announcementId);
}
