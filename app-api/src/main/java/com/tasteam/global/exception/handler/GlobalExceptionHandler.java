package com.tasteam.global.exception.handler;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.tasteam.global.dto.api.ErrorResponse;
import com.tasteam.global.dto.api.FieldErrorResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.external.ExternalServiceException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * 비즈니스 예외 처리 핸들러.
	 * <p>
	 * 도메인 로직에서 예상 가능한 오류(검증 실패, 권한 부족 등)를 처리한다.
	 * {@link BusinessException} 에 포함된 에러 정보를 기반으로
	 * HTTP 응답 코드와 에러 응답 바디를 생성한다.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse<?>> handleBusinessException(BusinessException e) {
		String errorCodeMessage = e.getErrorCode();
		String errorMessage = e.getErrorMessage();

		ErrorResponse<Void> response = ErrorResponse.of(errorCodeMessage, errorMessage);
		return ResponseEntity.status(e.getHttpStatus()).body(response);
	}

	@ExceptionHandler(ExternalServiceException.class)
	public ResponseEntity<ErrorResponse<?>> handleExternalServiceException(ExternalServiceException e) {
		String errorCodeMessage = e.getErrorCode();
		String errorMessage = e.getErrorMessage();

		ErrorResponse<Void> response = ErrorResponse.of(errorCodeMessage, errorMessage);
		return ResponseEntity.status(e.getHttpStatus()).body(response);
	}

	/**
	 * 요청 바인딩 / DTO 검증 실패 처리 핸들러.
	 * <p>
	 * {@link MethodArgumentNotValidException}, {@link BindException} 에서 발생한
	 * 필드 단위 오류 정보를 {@link FieldErrorResponse} 리스트로 변환하여 응답한다.
	 */
	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ErrorResponse<?>> handleValidationException(BindException e) {
		List<FieldErrorResponse> errors = e.getBindingResult().getFieldErrors().stream()
			.map(fieldError -> FieldErrorResponse.of(
				fieldError.getField(),
				fieldError.getDefaultMessage(),
				fieldError.getRejectedValue()))
			.toList();

		ErrorResponse<List<FieldErrorResponse>> response = ErrorResponse.of(
			"INVALID_REQUEST",
			"요청 값이 올바르지 않습니다.",
			errors);
		return ResponseEntity.badRequest().body(response);
	}

	/**
	 * 경로 변수/쿼리 파라미터 타입 불일치 처리 핸들러.
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse<?>> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
		FieldErrorResponse error = FieldErrorResponse.of(
			e.getName(),
			"요청 값의 타입이 올바르지 않습니다.",
			e.getValue());

		ErrorResponse<List<FieldErrorResponse>> response = ErrorResponse.of(
			"INVALID_REQUEST",
			"요청 값이 올바르지 않습니다.",
			List.of(error));
		return ResponseEntity.badRequest().body(response);
	}

	/**
	 * 처리되지 않은 모든 예외에 대한 최종 방어선 핸들러.
	 * <p>
	 * 예상하지 못한 서버 내부 오류를 500 상태 코드와 함께 반환한다.
	 * 상세 로깅 및 알림은 AOP 또는 로깅 설정에서 담당하며,
	 * 여기서는 클라이언트에 공통 에러 응답을 내려주는 역할만 수행한다.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse<?>> handleException(Exception e) {
		log.error("Unhandled exception", e);
		ErrorResponse<Void> response = ErrorResponse.of("서버 내부 오류가 발생했습니다");
		return ResponseEntity.internalServerError().body(response);
	}
}
