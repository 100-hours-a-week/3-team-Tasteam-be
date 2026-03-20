package com.tasteam.domain.review.entity;

import org.hibernate.annotations.Comment;
import org.springframework.util.Assert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "keyword", uniqueConstraints = {
	@UniqueConstraint(name = "uq_keyword_type_name", columnNames = {"type", "name"})
})
@Comment("리뷰에 사용되는 키워드 사전")
public class Keyword {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 50)
	private KeywordType type;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	public static Keyword create(KeywordType type, String name) {
		validateCreate(type, name);
		return Keyword.builder()
			.type(type)
			.name(name)
			.build();
	}

	private static void validateCreate(KeywordType type, String name) {
		Assert.notNull(type, "키워드 타입은 필수입니다");
		Assert.hasText(name, "키워드 이름은 필수입니다");
		if (name.length() > 200) {
			throw new IllegalArgumentException("키워드 이름이 너무 깁니다");
		}
	}
}
