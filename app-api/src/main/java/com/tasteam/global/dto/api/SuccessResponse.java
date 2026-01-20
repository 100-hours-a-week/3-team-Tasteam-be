package com.tasteam.global.dto.api;

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
public class SuccessResponse<T> {

	private boolean success;
	private T data;

	public static <T> SuccessResponse<T> success(T data) {
		return SuccessResponse.<T>builder()
			.success(true)
			.data(data)
			.build();
	}

	public static SuccessResponse<Void> success() {
		return SuccessResponse.<Void>builder()
			.success(true)
			.build();
	}

}
