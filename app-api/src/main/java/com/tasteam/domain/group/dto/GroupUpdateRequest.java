package com.tasteam.domain.group.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GroupUpdateRequest {

	private JsonNode name;
	private JsonNode address;
	private JsonNode detailAddress;
	private JsonNode emailDomain;
	private JsonNode status;

	@JsonProperty("logoImageURL")
	private JsonNode logoImageUrl;
}
