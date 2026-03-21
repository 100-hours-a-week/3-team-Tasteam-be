package com.tasteam.domain.file.validation;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.tasteam.domain.file.config.FileUploadPolicyProperties;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AllowedContentTypeValidator implements ConstraintValidator<AllowedContentType, String> {

	private final ObjectProvider<FileUploadPolicyProperties> uploadPolicyPropertiesProvider;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		FileUploadPolicyProperties properties = uploadPolicyPropertiesProvider
			.getIfAvailable(FileUploadPolicyProperties::new);
		return properties.isAllowedContentType(value);
	}
}
