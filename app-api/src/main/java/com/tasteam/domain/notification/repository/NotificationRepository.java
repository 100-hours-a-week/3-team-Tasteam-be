package com.tasteam.domain.notification.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	Page<Notification> findAllByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);

	Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

	long countByMemberIdAndReadAtIsNull(Long memberId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		UPDATE Notification n
		SET n.readAt = :readAt
		WHERE n.memberId = :memberId
		  AND n.readAt IS NULL
		""")
	int markAllAsRead(@Param("memberId")
	Long memberId, @Param("readAt")
	Instant readAt);
}
