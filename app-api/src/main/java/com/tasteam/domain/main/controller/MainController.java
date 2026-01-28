package com.tasteam.domain.main.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.main.controller.docs.MainControllerDocs;
import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.main.service.MainService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/main")
@RequiredArgsConstructor
public class MainController implements MainControllerDocs {

	private final MainService mainService;

	@GetMapping
	public SuccessResponse<MainPageResponse> getMain(
		@CurrentUser
		Long memberId,
		@Valid @ModelAttribute
		MainPageRequest request) {
		return SuccessResponse.success(mainService.getMain(memberId, request));
	}
}
