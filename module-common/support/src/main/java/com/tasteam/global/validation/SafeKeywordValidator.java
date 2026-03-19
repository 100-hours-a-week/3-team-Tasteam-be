package com.tasteam.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SafeKeywordValidator implements ConstraintValidator<SafeKeyword, String> {

	private boolean allowBlank;

	@Override
	public void initialize(SafeKeyword constraintAnnotation) {
		this.allowBlank = constraintAnnotation.allowBlank();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}

		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return allowBlank;
		}

		return KeywordSecurityPolicy.isSafeKeyword(trimmed);
	}
}
