package com.tasteam.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.controller.docs.AdminUserActivityEventControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminUserActivityEventSearchCondition;
import com.tasteam.domain.admin.dto.response.AdminUserActivityEventListItem;
import com.tasteam.domain.admin.service.AdminUserActivityEventService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/user-activity/events")
@Validated
public class AdminUserActivityEventController implements AdminUserActivityEventControllerDocs {

	private final AdminUserActivityEventService adminUserActivityEventService;

	@Override
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Page<AdminUserActivityEventListItem>> getEvents(
		@ModelAttribute
		AdminUserActivityEventSearchCondition condition,
		@PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
		Pageable pageable) {

		return SuccessResponse.success(adminUserActivityEventService.getEvents(condition, pageable));
	}
}
