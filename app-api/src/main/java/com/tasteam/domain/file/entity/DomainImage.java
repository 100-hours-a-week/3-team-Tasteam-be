package com.tasteam.domain.file.entity;

import java.util.Objects;

import org.springframework.util.Assert;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "domain_image", uniqueConstraints = {
	@UniqueConstraint(name = "uq_domain_image_link", columnNames = {"domain_type", "domain_id", "image_id"})
})
public class DomainImage extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "domain_type", nullable = false, length = 32)
	private DomainType domainType;

	@Column(name = "domain_id", nullable = false)
	private Long domainId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "image_id", nullable = false)
	private Image image;

	@Column(name = "sort_order")
	private Integer sortOrder;

	public static DomainImage create(DomainType domainType, Long domainId, Image image, Integer sortOrder) {
		validateCreate(domainType, domainId, image);
		return DomainImage.builder()
			.domainType(domainType)
			.domainId(domainId)
			.image(image)
			.sortOrder(Objects.requireNonNullElse(sortOrder, 0))
			.build();
	}

	public void changeSortOrder(Integer sortOrder) {
		this.sortOrder = Objects.requireNonNullElse(sortOrder, 0);
	}

	public void replaceImage(Image newImage) {
		Assert.notNull(newImage, "새 이미지는 필수입니다");
		this.image = newImage;
	}

	private static void validateCreate(DomainType domainType, Long domainId, Image image) {
		Assert.notNull(domainType, "도메인 타입은 필수입니다");
		Assert.notNull(domainId, "도메인 ID는 필수입니다");
		Assert.notNull(image, "이미지 정보는 필수입니다");
		if (domainId <= 0) {
			throw new IllegalArgumentException("도메인 ID는 양수여야 합니다");
		}
	}
}
