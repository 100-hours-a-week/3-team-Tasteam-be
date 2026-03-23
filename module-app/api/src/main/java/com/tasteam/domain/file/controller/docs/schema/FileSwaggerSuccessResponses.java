package com.tasteam.domain.file.controller.docs.schema;

import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageDetailResponse;
import com.tasteam.domain.file.dto.response.ImageSummaryResponse;
import com.tasteam.domain.file.dto.response.ImageUrlResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;

import io.swagger.v3.oas.annotations.media.Schema;

public final class FileSwaggerSuccessResponses {

	private FileSwaggerSuccessResponses() {}

	@Schema(name = "PresignedUploadSuccessResponse", description = "Presigned 업로드 생성 성공 응답")
	public static final class PresignedUploadSuccessResponse {

		@Schema(example = "true")
		public Boolean success;

		public PresignedUploadResponse data;
	}

	@Schema(name = "DomainImageLinkSuccessResponse", description = "도메인 이미지 연결 성공 응답")
	public static final class DomainImageLinkSuccessResponse {

		@Schema(example = "true")
		public Boolean success;

		public DomainImageLinkResponse data;
	}

	@Schema(name = "ImageDetailSuccessResponse", description = "이미지 상세 조회 성공 응답")
	public static final class ImageDetailSuccessResponse {

		@Schema(example = "true")
		public Boolean success;

		public ImageDetailResponse data;
	}

	@Schema(name = "ImageUrlSuccessResponse", description = "이미지 URL 조회 성공 응답")
	public static final class ImageUrlSuccessResponse {

		@Schema(example = "true")
		public Boolean success;

		public ImageUrlResponse data;
	}

	@Schema(name = "ImageSummarySuccessResponse", description = "이미지 요약 조회 성공 응답")
	public static final class ImageSummarySuccessResponse {

		@Schema(example = "true")
		public Boolean success;

		public ImageSummaryResponse data;
	}
}
