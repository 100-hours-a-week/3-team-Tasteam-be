package com.tasteam.domain.file.controller.docs;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.file.controller.docs.schema.FileSwaggerSuccessResponses.DomainImageLinkSuccessResponse;
import com.tasteam.domain.file.controller.docs.schema.FileSwaggerSuccessResponses.ImageDetailSuccessResponse;
import com.tasteam.domain.file.controller.docs.schema.FileSwaggerSuccessResponses.ImageSummarySuccessResponse;
import com.tasteam.domain.file.controller.docs.schema.FileSwaggerSuccessResponses.ImageUrlSuccessResponse;
import com.tasteam.domain.file.controller.docs.schema.FileSwaggerSuccessResponses.PresignedUploadSuccessResponse;
import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.ImageSummaryRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.file.FileSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "File", description = "파일 업로드/연결/조회 API")
public interface FileControllerDocs {

	@Operation(summary = "Presigned 업로드 생성", description = "Presigned POST 정보를 생성합니다.")
	@RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = PresignedUploadRequest.class)))
	@ApiResponse(responseCode = "200", description = "Presigned 업로드 생성 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PresignedUploadSuccessResponse.class), examples = @ExampleObject(name = "success", value = "{\"success\":true,\"data\":{\"uploads\":[{\"fileUuid\":\"a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012\",\"objectKey\":\"uploads/temp/a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012.jpg\",\"url\":\"https://my-bucket.s3.ap-northeast-2.amazonaws.com\",\"fields\":{\"key\":\"uploads/temp/a3f1c9e0-7a9b-4e9c-bc2e-1f2c33aa9012.jpg\",\"policy\":\"base64-policy\",\"x-amz-algorithm\":\"AWS4-HMAC-SHA256\",\"x-amz-credential\":\"AKIA.../20260130/ap-northeast-2/s3/aws4_request\",\"x-amz-date\":\"20260130T010000Z\",\"x-amz-signature\":\"abc123\",\"Content-Type\":\"image/jpeg\"},\"expiresAt\":\"2026-01-30T01:05:00Z\"}]}}")))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "PRESIGNED_UPLOADS")
	SuccessResponse<?> createPresignedUploads(@Validated
	PresignedUploadRequest request);

	@Operation(summary = "도메인 이미지 연결", description = "도메인과 파일을 연결하고 상태를 ACTIVE로 전환합니다.")
	@RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = DomainImageLinkRequest.class)))
	@ApiResponse(responseCode = "200", description = "도메인 이미지 연결 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DomainImageLinkSuccessResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "DOMAIN_LINK")
	SuccessResponse<?> linkDomainImage(@Validated
	DomainImageLinkRequest request);

	@Operation(summary = "이미지 상세 조회", description = "fileUuid 기준 상세 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "이미지 상세 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageDetailSuccessResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "IMAGE_DETAIL")
	SuccessResponse<?> getImageDetail(
		@Parameter(description = "파일 UUID", example = "c0a8012e-1e6f-4c0b-9f4f-1234567890ab") @PathVariable
		String fileUuid);

	@Operation(summary = "이미지 URL 조회", description = "fileUuid 기준 공개 URL을 조회합니다. (ACTIVE 상태만 허용)")
	@ApiResponse(responseCode = "200", description = "이미지 URL 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageUrlSuccessResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "IMAGE_DETAIL")
	SuccessResponse<?> getImageUrl(
		@Parameter(description = "파일 UUID", example = "c0a8012e-1e6f-4c0b-9f4f-1234567890ab") @PathVariable
		String fileUuid);

	@Operation(summary = "도메인용 이미지 요약 조회", description = "fileUuid 목록으로 요약 정보를 조회합니다.")
	@RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageSummaryRequest.class)))
	@ApiResponse(responseCode = "200", description = "이미지 요약 조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ImageSummarySuccessResponse.class)))
	@CustomErrorResponseDescription(value = FileSwaggerErrorResponseDescription.class, group = "IMAGE_SUMMARY")
	SuccessResponse<?> getImageSummary(@Validated
	ImageSummaryRequest request);
}
