package com.tasteam.domain.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tasteam.domain.notification.entity.MemberNotificationPreference;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;

public interface NotificationPreferenceRepository extends JpaRepository<MemberNotificationPreference, Long> {

	List<MemberNotificationPreference> findAllByMemberId(Long memberId);

	List<MemberNotificationPreference> findAllByMemberIdInAndChannelAndNotificationType(
		Collection<Long> memberIds,
		NotificationChannel channel,
		NotificationType notificationType);

	List<MemberNotificationPreference> findAllByMemberIdAndChannelIn(
		Long memberId, Collection<NotificationChannel> channels);

	Optional<MemberNotificationPreference> findByMemberIdAndChannelAndNotificationType(
		Long memberId, NotificationChannel channel, NotificationType notificationType);
}
