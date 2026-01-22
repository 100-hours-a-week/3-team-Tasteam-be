package com.tasteam.domain.restaurant.entity;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.*;
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
@Table(name = "restaurant_address")
@Comment("음식점의 주소 정보를 관리하는 테이블")
public class RestaurantAddress extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false, unique = true)
	private Restaurant restaurant;

	@Column(name = "sido", length = 20)
	@Comment("시/도")
	private String sido;

	@Column(name = "sigungu", length = 30)
	@Comment("시/군/구")
	private String sigungu;

	@Column(name = "eupmyeondong", length = 30)
	@Comment("읍/면/동")
	private String eupmyeondong;

	@Column(name = "postal_code", length = 16)
	@Comment("우편번호")
	private String postalCode;

	public static RestaurantAddress create(
		Restaurant restaurant,
		String sido,
		String sigungu,
		String eupmyeondong,
		String postalCode) {
		return RestaurantAddress.builder()
			.restaurant(restaurant)
			.sido(sido)
			.sigungu(sigungu)
			.eupmyeondong(eupmyeondong)
			.postalCode(postalCode)
			.build();
	}

	public void changeAddress(String sido, String sigungu, String eupmyeondong, String postalCode) {
		this.sido = sido;
		this.sigungu = sigungu;
		this.eupmyeondong = eupmyeondong;
		this.postalCode = postalCode;
	}
}
