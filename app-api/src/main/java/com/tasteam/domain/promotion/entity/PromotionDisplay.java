package com.tasteam.domain.promotion.entity;

import java.time.Instant;

import org.hibernate.annotations.Check;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "promotion_display", uniqueConstraints = @UniqueConstraint(name = "uk_promotion_display_promotion_id", columnNames = "promotion_id"), indexes = {
	@Index(name = "idx_promotion_display_window", columnList = "display_enabled, display_start_at, display_end_at"),
	@Index(name = "idx_promotion_display_channel_priority", columnList = "display_channel, display_priority")
})
@Check(constraints = "display_start_at <= display_end_at")
@ToString(exclude = "promotion")
public class PromotionDisplay extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "promotion_id", nullable = false)
	private Promotion promotion;

	@Column(name = "promotion_id", nullable = false, insertable = false, updatable = false)
	private Long promotionId;

	@Column(name = "display_enabled", nullable = false)
	private boolean displayEnabled;

	@Column(name = "display_start_at", nullable = false)
	private Instant displayStartAt;

	@Column(name = "display_end_at", nullable = false)
	private Instant displayEndAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "display_channel", nullable = false, length = 20)
	private DisplayChannel displayChannel;

	@Column(name = "display_priority", nullable = false)
	private int displayPriority;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static PromotionDisplay create(
		Promotion promotion,
		boolean displayEnabled,
		Instant displayStartAt,
		Instant displayEndAt,
		DisplayChannel displayChannel,
		int displayPriority) {
		return PromotionDisplay.builder()
			.promotion(promotion)
			.promotionId(promotion.getId())
			.displayEnabled(displayEnabled)
			.displayStartAt(displayStartAt)
			.displayEndAt(displayEndAt)
			.displayChannel(displayChannel)
			.displayPriority(displayPriority)
			.build();
	}

	public DisplayStatus getDisplayStatus() {
		return DisplayStatus.calculate(
			displayEnabled,
			promotion.getPublishStatus(),
			displayStartAt,
			displayEndAt,
			Instant.now());
	}

	public void delete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
