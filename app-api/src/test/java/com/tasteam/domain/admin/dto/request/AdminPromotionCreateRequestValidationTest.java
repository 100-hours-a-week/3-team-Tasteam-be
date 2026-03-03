package com.tasteam.domain.admin.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.domain.promotion.entity.DisplayChannel;
import com.tasteam.domain.promotion.entity.PublishStatus;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("[유닛](Admin) AdminPromotionCreateRequest 검증 테스트")
class AdminPromotionCreateRequestValidationTest {

	private final Validator validator;

	AdminPromotionCreateRequestValidationTest() {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		this.validator = factory.getValidator();
	}

	@Test
	@DisplayName("splashImageUrl 이 비어있으면 검증에 실패한다")
	void validationFailsWhenSplashImageUrlIsBlank() {
		AdminPromotionCreateRequest request = new AdminPromotionCreateRequest(
			"제목",
			"내용",
			"https://example.com/landing",
			Instant.parse("2026-03-01T00:00:00Z"),
			Instant.parse("2026-03-31T00:00:00Z"),
			PublishStatus.PUBLISHED,
			true,
			Instant.parse("2026-03-01T00:00:00Z"),
			Instant.parse("2026-03-31T00:00:00Z"),
			DisplayChannel.BOTH,
			1,
			"https://example.com/banner.webp",
			" ",
			null,
			List.of());

		var violations = validator.validate(request);

		assertThat(violations)
			.anyMatch(violation -> violation.getPropertyPath().toString().equals("splashImageUrl"));
	}
}
