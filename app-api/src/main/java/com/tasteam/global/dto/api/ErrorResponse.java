package com.tasteam.global.dto.api;

import java.util.List;

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
public class ErrorResponse<E> {

	private boolean success;
	private String code;
	private String message;
	private E errors;

	public static ErrorResponse<Void> of(String message) {
		return ErrorResponse.<Void>builder()
			.success(false)
			.message(message)
			.build();
	}

	public static ErrorResponse<Void> of(String code, String message) {
		return ErrorResponse.<Void>builder()
			.success(false)
			.code(code)
			.message(message)
			.build();
	}

	public static ErrorResponse<List<FieldErrorResponse>> of(
			String code,
			String message,
			List<FieldErrorResponse> errors) {
		return ErrorResponse.<List<FieldErrorResponse>>builder()
			.success(false)
			.code(code)
			.message(message)
			.errors(errors)
			.build();
	}
}
