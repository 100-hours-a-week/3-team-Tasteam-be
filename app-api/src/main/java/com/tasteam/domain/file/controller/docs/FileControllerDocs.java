package com.tasteam.domain.file.controller.docs;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.ImageSummaryRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageDetailResponse;
import com.tasteam.domain.file.dto.response.ImageSummaryResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.file.FileSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "File", description = "파일 업로드/연결/조회 API")
public interface FileControllerDocs {

	@Operation(summary = "Presigned 업로드 생성", description = "Presigned POST 정보를 생성합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = PresignedUploadRequest.class)))
	@ApiResponse(responseCode = "200", description = "Presigned 업로드 생성 성공", content = @Content(schema = @Schema(implementation = PresignedUploadResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "PRESIGNED_UPLOADS")
	SuccessResponse<PresignedUploadResponse> createPresignedUploads(@Validated
	PresignedUploadRequest request);

	@Operation(summary = "도메인 이미지 연결", description = "도메인과 파일을 연결하고 상태를 ACTIVE로 전환합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = DomainImageLinkRequest.class)))
	@ApiResponse(responseCode = "200", description = "도메인 이미지 연결 성공", content = @Content(schema = @Schema(implementation = DomainImageLinkResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "DOMAIN_LINK")
	SuccessResponse<DomainImageLinkResponse> linkDomainImage(@Validated
	DomainImageLinkRequest request);

	@Operation(summary = "이미지 상세 조회", description = "fileUuid 기준 상세 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "이미지 상세 조회 성공", content = @Content(schema = @Schema(implementation = ImageDetailResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "IMAGE_DETAIL")
	SuccessResponse<ImageDetailResponse> getImageDetail(
		@Parameter(description = "파일 UUID", example = "c0a8012e-1e6f-4c0b-9f4f-1234567890ab") @PathVariable
		String fileUuid);

	@Operation(summary = "도메인용 이미지 요약 조회", description = "fileUuid 목록으로 요약 정보를 조회합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = ImageSummaryRequest.class)))
	@ApiResponse(responseCode = "200", description = "이미지 요약 조회 성공", content = @Content(schema = @Schema(implementation = ImageSummaryResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "IMAGE_SUMMARY")
	SuccessResponse<ImageSummaryResponse> getImageSummary(@Validated
	ImageSummaryRequest request);
}
