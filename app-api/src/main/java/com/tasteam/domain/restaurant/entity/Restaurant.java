package com.tasteam.domain.restaurant.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Comment;
import org.locationtech.jts.geom.Point;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant")
@Comment("음식점의 기본 정보를 저장하는 마스터 테이블")
public class Restaurant extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "name", nullable = false, length = 100)
	@Comment("빈 문자열 불가")
	private String name;

	@Column(name = "full_address", nullable = false, length = 255)
	private String fullAddress;

	@Column(name = "location", nullable = false, columnDefinition = "geometry(Point,4326)")
	@Comment("WGS84")
	private Point location;

	@Column(name = "deleted_at")
	@Comment("소프트 삭제")
	private Instant deletedAt;

	@OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
	@Default
	private List<RestaurantImage> images = new ArrayList<>();

	@OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY)
	@Default
	private List<RestaurantAddress> addresses = new ArrayList<>();
}
