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
@Table(name = "restaurant_weekly_schedule", indexes = @Index(name = "idx_restaurant_weekly_schedule_lookup", columnList = "restaurant_id, day_of_week, effective_from, effective_to"))
@Comment("음식점의 요일별 정기 영업 시간을 관리하는 테이블")
public class RestaurantWeeklySchedule extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurant_id", nullable = false)
	private Restaurant restaurant;

	@Column(name = "day_of_week", nullable = false)
	@Comment("요일 (1=월요일 ~ 7=일요일)")
	private Integer dayOfWeek;

	@Column(name = "open_time")
	@Comment("오픈 시간 (HH:mm)")
	private LocalTime openTime;

	@Column(name = "close_time")
	@Comment("마감 시간 (HH:mm)")
	private LocalTime closeTime;

	@Column(name = "is_closed", nullable = false)
	@Comment("휴무일 여부")
	private Boolean isClosed;

	@Column(name = "effective_from")
	@Comment("정책 유효 시작일")
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	@Comment("정책 유효 종료일")
	private LocalDate effectiveTo;

	public static RestaurantWeeklySchedule create(
		Restaurant restaurant,
		Integer dayOfWeek,
		LocalTime openTime,
		LocalTime closeTime,
		Boolean isClosed,
		LocalDate effectiveFrom,
		LocalDate effectiveTo) {
		return RestaurantWeeklySchedule.builder()
			.restaurant(restaurant)
			.dayOfWeek(dayOfWeek)
			.openTime(openTime)
			.closeTime(closeTime)
			.isClosed(isClosed)
			.effectiveFrom(effectiveFrom)
			.effectiveTo(effectiveTo)
			.build();
	}

	public void changeSchedule(LocalTime openTime, LocalTime closeTime, Boolean isClosed) {
		this.openTime = openTime;
		this.closeTime = closeTime;
		this.isClosed = isClosed;
	}

	public void changeEffectivePeriod(LocalDate effectiveFrom, LocalDate effectiveTo) {
		this.effectiveFrom = effectiveFrom;
		this.effectiveTo = effectiveTo;
	}
}
