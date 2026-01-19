package com.tasteam.domain.common;

import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity extends BaseCreatedAtEntity {

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
