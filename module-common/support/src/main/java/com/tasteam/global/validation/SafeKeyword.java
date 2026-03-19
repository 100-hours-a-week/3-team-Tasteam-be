package com.tasteam.global.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = SafeKeywordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeKeyword{

	String message() default "검색 키워드에 허용되지 않는 문자열이 포함되어 있습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	boolean allowBlank() default false;
}
