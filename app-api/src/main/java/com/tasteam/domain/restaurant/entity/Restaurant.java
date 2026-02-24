package com.tasteam.domain.restaurant.entity;

import java.time.Instant;

import org.hibernate.annotations.Comment;
import org.locationtech.jts.geom.Point;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

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

	@Column(name = "location", columnDefinition = "geometry(Point,4326)")
	@Comment("WGS84")
	private Point location;

	@Column(name = "phone_number", length = 100)
	private String phoneNumber;

	@Column(name = "deleted_at")
	@Comment("소프트 삭제")
	private Instant deletedAt;

	@Column(name = "vector_epoch", nullable = false)
	@Comment("벡터 업로드 버전; 업로드 성공 시마다 1 증가")
	private Long vectorEpoch;

	@Column(name = "vector_synced_at")
	@Comment("마지막 벡터 업로드 완료 시각")
	private Instant vectorSyncedAt;

	public static Restaurant create(String name, String fullAddress, Point location, String phoneNumber) {
		return Restaurant.builder()
			.name(name)
			.fullAddress(fullAddress)
			.location(location)
			.phoneNumber(phoneNumber)
			.deletedAt(null)
			.vectorEpoch(0L)
			.vectorSyncedAt(null)
			.build();
	}

	public void changeName(String name) {
		this.name = name;
	}

	public void softDelete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	/**
	 * 벡터 업로드 성공 시 호출. vector_epoch를 1 증가시키고 vector_synced_at을 갱신한다.
	 */
	public void incrementVectorEpoch(Instant syncedAt) {
		this.vectorEpoch = this.vectorEpoch == null ? 1L : this.vectorEpoch + 1;
		this.vectorSyncedAt = syncedAt;
	}
}
