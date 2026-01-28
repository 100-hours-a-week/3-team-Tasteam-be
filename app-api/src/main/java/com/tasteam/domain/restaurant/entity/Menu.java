package com.tasteam.domain.restaurant.entity;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "menu", indexes = @Index(name = "idx_menu_category_order", columnList = "category_id, display_order"))
@Comment("음식점의 메뉴 항목을 관리하는 테이블")
public class Menu extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private MenuCategory category;

	@Column(name = "name", nullable = false, length = 100)
	@Comment("메뉴 이름")
	private String name;

	@Column(name = "description", length = 500)
	@Comment("메뉴 설명")
	private String description;

	@Column(name = "price", nullable = false)
	@Comment("메뉴 가격 (0 이상 정수)")
	private Integer price;

	@Column(name = "image_url", length = 500)
	@Comment("메뉴 이미지 URL")
	private String imageUrl;

	@Column(name = "is_recommended", nullable = false)
	@Comment("추천 메뉴 여부")
	private Boolean isRecommended;

	@Column(name = "display_order", nullable = false)
	@Comment("노출 순서 (0 이상 정수)")
	private Integer displayOrder;

	public static Menu create(
		MenuCategory category,
		String name,
		String description,
		Integer price,
		String imageUrl,
		Boolean isRecommended,
		Integer displayOrder) {
		return Menu.builder()
			.category(category)
			.name(name)
			.description(description)
			.price(price)
			.imageUrl(imageUrl)
			.isRecommended(isRecommended != null ? isRecommended : false)
			.displayOrder(displayOrder)
			.build();
	}

	public void changeName(String name) {
		this.name = name;
	}

	public void changeDescription(String description) {
		this.description = description;
	}

	public void changePrice(Integer price) {
		this.price = price;
	}

	public void changeImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public void changeIsRecommended(Boolean isRecommended) {
		this.isRecommended = isRecommended;
	}

	public void changeDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
