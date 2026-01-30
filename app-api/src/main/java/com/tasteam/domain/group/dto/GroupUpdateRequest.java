package com.tasteam.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record GroupUpdateRequest(
	JsonNode name,
	JsonNode address,
	JsonNode detailAddress,
	JsonNode emailDomain,
	JsonNode status,
	@JsonProperty("logoImageId")
	JsonNode logoImageId) {
}
