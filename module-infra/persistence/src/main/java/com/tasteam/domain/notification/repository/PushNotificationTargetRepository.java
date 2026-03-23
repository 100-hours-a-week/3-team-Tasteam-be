package com.tasteam.domain.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.notification.entity.PushNotificationTarget;

public interface PushNotificationTargetRepository extends JpaRepository<PushNotificationTarget, Long> {

	Optional<PushNotificationTarget> findByFcmToken(String fcmToken);

	Optional<PushNotificationTarget> findByMemberIdAndDeviceId(Long memberId, String deviceId);

	List<PushNotificationTarget> findAllByMemberId(Long memberId);

	List<PushNotificationTarget> findAllByMemberIdIn(Collection<Long> memberIds);

	void deleteByFcmToken(String fcmToken);
}
