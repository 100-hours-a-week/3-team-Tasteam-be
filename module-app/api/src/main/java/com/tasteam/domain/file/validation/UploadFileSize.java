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
@Constraint(validatedBy = UploadFileSizeValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface UploadFileSize{

	String message() default "size가 허용된 범위를 벗어났습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
