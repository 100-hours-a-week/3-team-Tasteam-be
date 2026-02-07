package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiReviewModel(
	Integer id,
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("member_id")
	Integer memberId,
	@JsonProperty("group_id")
	Integer groupId,
	@JsonProperty("subgroup_id")
	Integer subgroupId,
	String content,
	@JsonProperty("is_recommended")
	Boolean isRecommended,
	@JsonProperty("created_at")
	String createdAt,
	@JsonProperty("updated_at")
	String updatedAt) {
}
