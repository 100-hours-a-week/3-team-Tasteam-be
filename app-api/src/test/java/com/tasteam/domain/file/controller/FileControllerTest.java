package com.tasteam.domain.file.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.ControllerWebMvcTest;
import com.tasteam.domain.file.dto.request.DomainImageLinkRequest;
import com.tasteam.domain.file.dto.request.ImageSummaryRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadFileRequest;
import com.tasteam.domain.file.dto.request.PresignedUploadRequest;
import com.tasteam.domain.file.dto.response.DomainImageLinkResponse;
import com.tasteam.domain.file.dto.response.ImageDetailResponse;
import com.tasteam.domain.file.dto.response.ImageSummaryItem;
import com.tasteam.domain.file.dto.response.ImageSummaryResponse;
import com.tasteam.domain.file.dto.response.ImageUrlResponse;
import com.tasteam.domain.file.dto.response.LinkedDomainResponse;
import com.tasteam.domain.file.dto.response.PresignedUploadItem;
import com.tasteam.domain.file.dto.response.PresignedUploadResponse;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.entity.FilePurpose;
import com.tasteam.domain.file.service.FileService;

@ControllerWebMvcTest(FileController.class)
class FileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private FileService fileService;

	@Nested
	@DisplayName("프리사인드 업로드 URL 생성")
	class CreatePresignedUploads {

		@Test
		@DisplayName("프리사인드 업로드 요청하면 업로드 URL 목록을 반환한다")
		void 프리사인드_업로드_성공() throws Exception {
			// given
			String fileUuid = UUID.randomUUID().toString();
			PresignedUploadResponse response = new PresignedUploadResponse(
				List.of(new PresignedUploadItem(
					fileUuid, "images/test.jpg", "https://s3.example.com/upload",
					Map.of("key", "value"), Instant.now().plusSeconds(3600))));

			given(fileService.createPresignedUploads(any())).willReturn(response);

			PresignedUploadRequest request = new PresignedUploadRequest(
				FilePurpose.REVIEW_IMAGE,
				List.of(new PresignedUploadFileRequest("test.jpg", "image/jpeg", 1024)));

			// when & then
			mockMvc.perform(post("/api/v1/files/uploads/presigned")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.uploads[0].fileUuid").value(fileUuid))
				.andExpect(jsonPath("$.data.uploads[0].url").value("https://s3.example.com/upload"));
		}
	}

	@Nested
	@DisplayName("도메인 이미지 연결")
	class LinkDomainImage {

		@Test
		@DisplayName("도메인 이미지를 연결하면 연결 정보를 반환한다")
		void 도메인_이미지_연결_성공() throws Exception {
			// given
			DomainImageLinkResponse response = new DomainImageLinkResponse(1L, "LINKED");

			given(fileService.linkDomainImage(any())).willReturn(response);

			DomainImageLinkRequest request = new DomainImageLinkRequest(
				DomainType.RESTAURANT, 1L, UUID.randomUUID().toString(), 0);

			// when & then
			mockMvc.perform(post("/api/v1/files/domain-links")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.domainImageId").value(1))
				.andExpect(jsonPath("$.data.imageStatus").value("LINKED"));
		}
	}

	@Nested
	@DisplayName("이미지 상세 조회")
	class GetImageDetail {

		@Test
		@DisplayName("파일 UUID로 이미지 상세 정보를 조회한다")
		void 이미지_상세_조회_성공() throws Exception {
			// given
			String fileUuid = UUID.randomUUID().toString();
			ImageDetailResponse response = new ImageDetailResponse(
				fileUuid, "UPLOADED", "REVIEW_IMAGE", Instant.now(),
				List.of(new LinkedDomainResponse("RESTAURANT", 1L, 0, Instant.now())));

			given(fileService.getImageDetail(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/files/{fileUuid}", fileUuid))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.fileUuid").value(fileUuid))
				.andExpect(jsonPath("$.data.status").value("UPLOADED"))
				.andExpect(jsonPath("$.data.linkedDomains[0].domainType").value("RESTAURANT"));
		}
	}

	@Nested
	@DisplayName("이미지 URL 조회")
	class GetImageUrl {

		@Test
		@DisplayName("파일 UUID로 이미지 공개 URL을 조회한다")
		void 이미지_URL_조회_성공() throws Exception {
			// given
			String fileUuid = UUID.randomUUID().toString();
			ImageUrlResponse response = new ImageUrlResponse(fileUuid, "https://cdn.example.com/uploads/temp/a.jpg");

			given(fileService.getImageUrl(any())).willReturn(response);

			// when & then
			mockMvc.perform(get("/api/v1/files/{fileUuid}/url", fileUuid))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.fileUuid").value(fileUuid))
				.andExpect(jsonPath("$.data.url").value("https://cdn.example.com/uploads/temp/a.jpg"));
		}
	}

	@Nested
	@DisplayName("이미지 요약 조회")
	class GetImageSummary {

		@Test
		@DisplayName("파일 UUID 목록으로 이미지 요약을 조회한다")
		void 이미지_요약_조회_성공() throws Exception {
			// given
			String fileUuid = UUID.randomUUID().toString();
			ImageSummaryResponse response = new ImageSummaryResponse(
				List.of(new ImageSummaryItem(1L, fileUuid, "https://example.com/img.jpg")));

			given(fileService.getImageSummary(any())).willReturn(response);

			ImageSummaryRequest request = new ImageSummaryRequest(List.of(fileUuid));

			// when & then
			mockMvc.perform(post("/api/v1/files/summary")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].fileUuid").value(fileUuid))
				.andExpect(jsonPath("$.data.items[0].url").value("https://example.com/img.jpg"));
		}
	}
}
