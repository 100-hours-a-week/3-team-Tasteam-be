package com.tasteam.domain.review.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("키워드 엔티티")
class KeywordTest {

	@Nested
	@DisplayName("키워드 생성")
	class CreateKeyword {

		@Test
		@DisplayName("유효한 타입과 이름으로 키워드를 생성한다")
		void create_validParams_createsKeyword() {
			Keyword keyword = Keyword.create(KeywordType.POSITIVE_ASPECT, "맛있어요");

			assertThat(keyword.getType()).isEqualTo(KeywordType.POSITIVE_ASPECT);
			assertThat(keyword.getName()).isEqualTo("맛있어요");
		}

		@Test
		@DisplayName("키워드 타입이 null이면 생성에 실패한다")
		void create_nullType_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Keyword.create(null, "맛있어요"))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("키워드 이름이 빈 문자열이면 생성에 실패한다")
		void create_blankName_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Keyword.create(KeywordType.POSITIVE_ASPECT, "  "))
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		@DisplayName("키워드 이름이 최대 길이를 초과하면 생성에 실패한다")
		void create_nameTooLong_throwsIllegalArgumentException() {
			assertThatThrownBy(() -> Keyword.create(KeywordType.POSITIVE_ASPECT, "a".repeat(201)))
				.isInstanceOf(IllegalArgumentException.class);
		}
	}
}
