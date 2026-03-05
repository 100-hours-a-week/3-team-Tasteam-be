package com.tasteam.domain.admin.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.batch.dummy.DummySeedJobTracker;
import com.tasteam.batch.dummy.service.DummyDataSeedService;
import com.tasteam.domain.admin.controller.docs.AdminDummyControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.domain.admin.dto.response.AdminDataCountResponse;
import com.tasteam.domain.admin.dto.response.DummySeedStatusResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Profile("!prod")
@RequestMapping("/api/v1/admin/dummy")
public class AdminDummyController implements AdminDummyControllerDocs {

	private final DummyDataSeedService dummyDataSeedService;
	private final DummySeedJobTracker jobTracker;

	@Override
	@PostMapping("/seed")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void startSeed(
		@RequestBody @Valid
		AdminDummySeedRequest request) {

		if (jobTracker.isRunning()) {
			throw new BusinessException(CommonErrorCode.SEED_ALREADY_RUNNING);
		}
		dummyDataSeedService.seedAsync(request);
	}

	@Override
	@GetMapping("/seed/status")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<DummySeedStatusResponse> getSeedStatus() {
		return SuccessResponse.success(jobTracker.getSnapshot());
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
