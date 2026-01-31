package com.tasteam.domain.restaurant.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Deprecated
@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant_image")
@Comment("음식점 대표 이미지 - DomainImage 사용으로 대체됨")
public class RestaurantImage extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(name = "image_url", nullable = false, length = 500)
	@Comment("빈 문자열 불가")
	private String imageUrl;

	@Column(name = "sort_order", nullable = false)
	@Comment("양의 정수")
	private int sortOrder;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static RestaurantImage create(Restaurant restaurant, String imageUrl, int sortOrder) {
		return RestaurantImage.builder()
			.restaurant(restaurant)
			.imageUrl(imageUrl)
			.sortOrder(sortOrder)
			.deletedAt(null)
			.build();
	}

	public void changeSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void softDelete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
