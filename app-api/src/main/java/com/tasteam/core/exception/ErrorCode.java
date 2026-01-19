package com.tasteam.core.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
	HttpStatus getHttpStatus();

	String getMessage();

	default String name() {
		if (this instanceof Enum<?>) {
			return ((Enum<?>)this).name();
		}
		return this.getClass().getSimpleName();
	}
}
