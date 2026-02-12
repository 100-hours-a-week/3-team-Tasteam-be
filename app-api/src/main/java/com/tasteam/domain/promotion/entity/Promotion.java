package com.tasteam.domain.promotion.entity;

import java.time.Instant;

import org.hibernate.annotations.Check;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "promotion")
@Check(constraints = "promotion_start_at <= promotion_end_at")
public class Promotion extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "landing_url", length = 500)
	private String landingUrl;

	@Column(name = "promotion_start_at", nullable = false)
	private Instant promotionStartAt;

	@Column(name = "promotion_end_at", nullable = false)
	private Instant promotionEndAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "publish_status", nullable = false, length = 20)
	private PublishStatus publishStatus;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static Promotion create(
		String title,
		String content,
		String landingUrl,
		Instant promotionStartAt,
		Instant promotionEndAt,
		PublishStatus publishStatus) {
		return Promotion.builder()
			.title(title)
			.content(content)
			.landingUrl(landingUrl)
			.promotionStartAt(promotionStartAt)
			.promotionEndAt(promotionEndAt)
			.publishStatus(publishStatus)
			.build();
	}

	public PromotionStatus getPromotionStatus() {
		return PromotionStatus.calculate(promotionStartAt, promotionEndAt, Instant.now());
	}

	public void delete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}

	public void changeBasicInfo(String title, String content, String landingUrl) {
		if (title != null) {
			this.title = title;
		}
		if (content != null) {
			this.content = content;
		}
		if (landingUrl != null) {
			this.landingUrl = landingUrl;
		}
	}

	public void changeSchedule(Instant promotionStartAt, Instant promotionEndAt) {
		if (promotionStartAt != null) {
			this.promotionStartAt = promotionStartAt;
		}
		if (promotionEndAt != null) {
			this.promotionEndAt = promotionEndAt;
		}
	}

	public void changePublishStatus(PublishStatus publishStatus) {
		this.publishStatus = publishStatus;
	}
}
