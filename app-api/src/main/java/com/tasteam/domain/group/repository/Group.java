package com.tasteam.domain.group.repository;

import java.time.Instant;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "\"group\"")
public class Group extends BaseTimeEntity {

	@Id
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
	private Object location;

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
}
