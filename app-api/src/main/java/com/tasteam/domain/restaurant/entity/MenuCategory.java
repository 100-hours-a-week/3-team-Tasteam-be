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
@Table(name = "menu_category", indexes = @Index(name = "idx_menu_category_restaurant_order", columnList = "restaurant_id, display_order"))
@Comment("음식점의 메뉴 카테고리를 관리하는 테이블")
public class MenuCategory extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(name = "name", nullable = false, length = 50)
	@Comment("카테고리 이름 (예: 메인, 음료)")
	private String name;

	@Column(name = "display_order", nullable = false)
	@Comment("노출 순서 (0 이상 정수)")
	private Integer displayOrder;

	public static MenuCategory create(Restaurant restaurant, String name, Integer displayOrder) {
		return MenuCategory.builder()
			.restaurant(restaurant)
			.name(name)
			.displayOrder(displayOrder)
			.build();
	}

	public void changeName(String name) {
		this.name = name;
	}

	public void changeDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
}
