package com.tasteam.core.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldErrorResponse {

	private String field;
	private String reason;
	private Object rejectedValue;

	public static FieldErrorResponse of(String field, String reason, Object rejectedValue) {
		return FieldErrorResponse.builder()
			.field(field)
			.reason(reason)
			.rejectedValue(rejectedValue)
			.build();
	}
}
