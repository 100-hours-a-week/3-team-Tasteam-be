package com.tasteam.domain.admin.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.batch.dummy.service.DummyDataSeedService;
import com.tasteam.domain.admin.controller.docs.AdminDummyControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.domain.admin.dto.response.AdminDataCountResponse;
import com.tasteam.domain.admin.dto.response.AdminDummySeedResponse;
import com.tasteam.global.dto.api.ErrorResponse;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@Profile("!prod")
@RequestMapping("/api/v1/admin/dummy")
public class AdminDummyController implements AdminDummyControllerDocs {

	private final DummyDataSeedService dummyDataSeedService;

	@Override
	@PostMapping("/seed")
	public ResponseEntity<?> seed(
		@RequestBody @Valid
		AdminDummySeedRequest request) {

		try {
			AdminDummySeedResponse result = dummyDataSeedService.seed(request);
			return ResponseEntity.ok(SuccessResponse.success(result));
		} catch (DummyDataSeedService.DummySeedStepException e) {
			log.error("[AdminDummyController] seed step failed: {}", e.getStepName(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of("SEED_STEP_FAILED", e.getMessage()));
		} catch (Exception e) {
			log.error("[AdminDummyController] seed failed", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.of("SEED_FAILED", "씨딩 중 오류가 발생했습니다: " + e.getMessage()));
		}
	}

	@Override
	@GetMapping("/count")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminDataCountResponse> count() {
		return SuccessResponse.success(dummyDataSeedService.count());
	}

	@Override
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteDummyData() {
		dummyDataSeedService.deleteDummyData();
	}
}
