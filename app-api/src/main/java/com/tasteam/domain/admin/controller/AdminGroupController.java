package com.tasteam.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.dto.request.AdminGroupCreateRequest;
import com.tasteam.domain.admin.dto.response.AdminGroupListItem;
import com.tasteam.domain.admin.policy.AdminAuthPolicy;
import com.tasteam.domain.admin.service.AdminGroupService;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/groups")
public class AdminGroupController {

	private final AdminGroupService adminGroupService;
	private final AdminAuthPolicy adminAuthPolicy;
	private final MemberRepository memberRepository;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Page<AdminGroupListItem>> getGroups(
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable,
		Authentication authentication) {

		Member member = getMemberFromAuth(authentication);
		adminAuthPolicy.validateAdmin(member);

		Page<AdminGroupListItem> result = adminGroupService.getGroups(pageable);
		return SuccessResponse.success(result);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Long> createGroup(
		@Validated @RequestBody
		AdminGroupCreateRequest request,
		Authentication authentication) {

		Member member = getMemberFromAuth(authentication);
		adminAuthPolicy.validateAdmin(member);

		Long groupId = adminGroupService.createGroup(request);
		return SuccessResponse.success(groupId);
	}

	private Member getMemberFromAuth(Authentication authentication) {
		if (authentication == null || authentication.getName() == null) {
			throw new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED);
		}
		return memberRepository.findByEmail(authentication.getName())
			.orElseThrow(() -> new BusinessException(CommonErrorCode.AUTHENTICATION_REQUIRED));
	}
}
