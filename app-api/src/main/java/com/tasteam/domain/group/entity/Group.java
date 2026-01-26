package com.tasteam.domain.group.entity;

import java.time.Instant;

import org.locationtech.jts.geom.Point;

import com.tasteam.domain.common.BaseTimeEntity;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.group.type.GroupStatus;
import com.tasteam.domain.group.type.GroupType;

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

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "\"group\"")
public class Group extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	@Column(name = "id", nullable = false)
	private Long id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private GroupType type;

	@Column(name = "logo_image_url", length = 500)
	private String logoImageUrl;

	@Column(name = "address", nullable = false, length = 255)
	private String address;

	@Column(name = "detail_address", length = 255)
	private String detailAddress;

	@Column(name = "location", columnDefinition = "geometry(Point,4326)")
	private Point location;

	@Enumerated(EnumType.STRING)
	@Column(name = "join_type", nullable = false, length = 20)
	private GroupJoinType joinType;

	@Column(name = "email_domain", length = 100)
	private String emailDomain;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private GroupStatus status;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public void updateName(String name) {
		this.name = name;
	}

	public void updateAddress(String address) {
		this.address = address;
	}

	public void updateDetailAddress(String detailAddress) {
		this.detailAddress = detailAddress;
	}

	public void updateEmailDomain(String emailDomain) {
		this.emailDomain = emailDomain;
	}

	public void updateStatus(GroupStatus status) {
		this.status = status;
	}

	public void updateLogoImageUrl(String logoImageUrl) {
		this.logoImageUrl = logoImageUrl;
	}

	public void delete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
