package com.tasteam.domain.promotion.entity;

import java.time.Instant;

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
@Table(name = "promotion_asset", indexes = @Index(name = "idx_promotion_asset_type_order", columnList = "promotion_id, asset_type, sort_order"))
public class PromotionAsset extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "promotion_id", nullable = false)
	private Promotion promotion;

	@Column(name = "promotion_id", nullable = false, insertable = false, updatable = false)
	private Long promotionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "asset_type", nullable = false, length = 30)
	private AssetType assetType;

	@Column(name = "image_url", nullable = false, length = 500)
	private String imageUrl;

	@Column(name = "alt_text", length = 200)
	private String altText;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "is_primary", nullable = false)
	private boolean isPrimary;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static PromotionAsset create(
		Promotion promotion,
		AssetType assetType,
		String imageUrl,
		String altText,
		int sortOrder,
		boolean isPrimary) {
		return PromotionAsset.builder()
			.promotion(promotion)
			.promotionId(promotion.getId())
			.assetType(assetType)
			.imageUrl(imageUrl)
			.altText(altText)
			.sortOrder(sortOrder)
			.isPrimary(isPrimary)
			.build();
	}

	public void delete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
