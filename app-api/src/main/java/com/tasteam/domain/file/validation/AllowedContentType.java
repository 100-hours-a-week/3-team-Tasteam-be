package com.tasteam.domain.file.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = AllowedContentTypeValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface AllowedContentType{

	String message() default "contentType이 허용된 타입이 아닙니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
