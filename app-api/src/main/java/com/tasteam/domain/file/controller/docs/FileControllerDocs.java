package com.tasteam.domain.file.controller.docs;

import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.ImageSummaryRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageDetailResponse;
import com.tasteam.domain.file.dto.response.ImageSummaryResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.global.dto.api.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "File", description = "파일 업로드/연결/조회 API")
public interface FileControllerDocs {

	@Operation(summary = "Presigned 업로드 생성", description = "Presigned POST 정보를 생성합니다.")
	SuccessResponse<PresignedUploadResponse> createPresignedUploads(PresignedUploadRequest request);

	@Operation(summary = "도메인 이미지 연결", description = "도메인과 파일을 연결하고 상태를 ACTIVE로 전환합니다.")
	SuccessResponse<DomainImageLinkResponse> linkDomainImage(DomainImageLinkRequest request);

	@Operation(summary = "이미지 상세 조회", description = "fileUuid 기준 상세 정보를 조회합니다.")
	SuccessResponse<ImageDetailResponse> getImageDetail(String fileUuid);

	@Operation(summary = "도메인용 이미지 요약 조회", description = "fileUuid 목록으로 요약 정보를 조회합니다.")
	SuccessResponse<ImageSummaryResponse> getImageSummary(ImageSummaryRequest request);
}
