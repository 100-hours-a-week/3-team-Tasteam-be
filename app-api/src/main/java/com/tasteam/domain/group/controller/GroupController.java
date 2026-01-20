package com.tasteam.domain.group.controller;

import com.tasteam.global.dto.api.SuccessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.service.GroupService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Validated
public class GroupController {

	private final GroupService groupService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<GroupCreateResponse> createGroup(@Valid @RequestBody GroupCreateRequest request) {
		return SuccessResponse.success(groupService.createGroup(request));
	}

	@GetMapping("/{groupId}")
	public SuccessResponse<GroupGetResponse> getGroup(@PathVariable @Positive Long groupId) {
		return SuccessResponse.success(groupService.getGroup(groupId));
	}
}
