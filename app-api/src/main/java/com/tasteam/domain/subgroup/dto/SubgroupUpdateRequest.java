package com.tasteam.domain.subgroup.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubgroupUpdateRequest {
	private JsonNode name;
	private JsonNode description;
	@JsonProperty("profileImageId")
	private JsonNode profileImageId;
}
