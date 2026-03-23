package com.tasteam.domain.restaurant.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.Comment;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "restaurant_schedule_override", uniqueConstraints = @UniqueConstraint(name = "uk_restaurant_schedule_override_restaurant_date", columnNames = {
	"restaurant_id", "date"}))
@Comment("음식점의 특정 날짜 예외 영업 시간을 관리하는 테이블")
public class RestaurantScheduleOverride extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(name = "date", nullable = false)
	@Comment("예외 적용 날짜")
	private LocalDate date;

	@Column(name = "open_time")
	@Comment("오픈 시간 (HH:mm)")
	private LocalTime openTime;

	@Column(name = "close_time")
	@Comment("마감 시간 (HH:mm)")
	private LocalTime closeTime;

	@Column(name = "is_closed", nullable = false)
	@Comment("휴무 여부")
	private Boolean isClosed;

	@Column(name = "reason", length = 255)
	@Comment("예외 사유")
	private String reason;

	public static RestaurantScheduleOverride create(
		Restaurant restaurant,
		LocalDate date,
		LocalTime openTime,
		LocalTime closeTime,
		Boolean isClosed,
		String reason) {
		return RestaurantScheduleOverride.builder()
			.restaurant(restaurant)
			.date(date)
			.openTime(openTime)
			.closeTime(closeTime)
			.isClosed(isClosed)
			.reason(reason)
			.build();
	}

	public void changeSchedule(LocalTime openTime, LocalTime closeTime, Boolean isClosed, String reason) {
		this.openTime = openTime;
		this.closeTime = closeTime;
		this.isClosed = isClosed;
		this.reason = reason;
	}
}
