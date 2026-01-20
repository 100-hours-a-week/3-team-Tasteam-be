package com.tasteam.domain.group.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.group.dto.GroupGetResponse;
import com.tasteam.domain.group.service.GroupService;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Validated
public class GroupController {

	private final GroupService groupService;

	@GetMapping("/{groupId}")
	public ResponseEntity<GroupGetResponse> getGroup(@PathVariable @Positive Long groupId) {
		return ResponseEntity.ok(groupService.getGroup(groupId));
	}
}
