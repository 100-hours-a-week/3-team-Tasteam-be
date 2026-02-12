package com.tasteam.domain.announcement.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "announcement")
public class Announcement extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	public static Announcement create(String title, String content) {
		return Announcement.builder()
			.title(title)
			.content(content)
			.build();
	}

	public void update(String title, String content) {
		if (title != null) {
			this.title = title;
		}
		if (content != null) {
			this.content = content;
		}
	}

	public void delete(Instant deletedAt) {
		this.deletedAt = deletedAt;
	}
}
