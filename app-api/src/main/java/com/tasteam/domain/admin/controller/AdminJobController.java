package com.tasteam.domain.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.batch.image.optimization.service.ImageOptimizationService;
import com.tasteam.batch.image.optimization.service.ImageOptimizationService.OptimizationResult;
import com.tasteam.domain.admin.dto.response.AdminJobResponse;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/jobs")
public class AdminJobController {

	private final ImageOptimizationService imageOptimizationService;

	@PostMapping("/image-optimization")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminJobResponse> runImageOptimization(
		@RequestParam(defaultValue = "100")
		int batchSize) {

		OptimizationResult result = imageOptimizationService.processOptimizationBatch(batchSize);

		return SuccessResponse.success(new AdminJobResponse(
			"image-optimization",
			result.successCount(),
			result.failedCount(),
			result.skippedCount()));
	}
}
