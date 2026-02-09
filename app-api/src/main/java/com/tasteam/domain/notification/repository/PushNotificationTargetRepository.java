package com.tasteam.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.notification.entity.PushNotificationTarget;

public interface PushNotificationTargetRepository extends JpaRepository<PushNotificationTarget, Long> {

	Optional<PushNotificationTarget> findByFcmToken(String fcmToken);

	List<PushNotificationTarget> findAllByMemberId(Long memberId);

	void deleteByFcmToken(String fcmToken);
}
