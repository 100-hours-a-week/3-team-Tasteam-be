package com.tasteam.domain.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.policy.AdminAuthPolicy;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.dto.request.MenuBulkCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCategoryCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCreateRequest;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.domain.restaurant.service.MenuService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/restaurants/{restaurantId}/menus")
public class AdminMenuController {

	private final MenuService menuService;
	private final AdminAuthPolicy adminAuthPolicy;
	private final MemberRepository memberRepository;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<RestaurantMenuResponse> getMenus(
		@PathVariable
		Long restaurantId,
		@RequestParam(required = false, defaultValue = "true")
		boolean includeEmptyCategories,
		@RequestParam(required = false, defaultValue = "false")
		boolean recommendedFirst,
		Authentication authentication) {

		Member member = getMemberFromAuth(authentication);
		adminAuthPolicy.validateAdmin(member);

		RestaurantMenuResponse result = menuService.getRestaurantMenus(
			restaurantId,
			includeEmptyCategories,
			recommendedFirst);
		return SuccessResponse.success(result);
	}

	@PostMapping("/categories")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Long> createMenuCategory(
		@PathVariable
		Long restaurantId,
		@Validated @RequestBody
		MenuCategoryCreateRequest request,
		Authentication authentication) {

		Member member = getMemberFromAuth(authentication);
		adminAuthPolicy.validateAdmin(member);

		Long categoryId = menuService.createMenuCategory(restaurantId, request);
		return SuccessResponse.success(categoryId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Long> createMenu(
		@PathVariable
		Long restaurantId,
		@Validated @RequestBody
		MenuCreateRequest request,
		Authentication authentication) {

		Member member = getMemberFromAuth(authentication);
		adminAuthPolicy.validateAdmin(member);

		Long menuId = menuService.createMenu(restaurantId, request);
		return SuccessResponse.success(menuId);
	}

	@PostMapping("/bulk")
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Void> createMenusBulk(
		@PathVariable
		Long restaurantId,
		@Validated @RequestBody
		MenuBulkCreateRequest request,
		Authentication authentication) {

		Member member = getMemberFromAuth(authentication);
		adminAuthPolicy.validateAdmin(member);

		menuService.createMenusBulk(restaurantId, request);
		return SuccessResponse.success(null);
	}

	private Member getMemberFromAuth(Authentication authentication) {
		if (authentication == null || authentication.getName() == null) {
			throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
		}
		return memberRepository.findByEmail(authentication.getName())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED));
	}
}
