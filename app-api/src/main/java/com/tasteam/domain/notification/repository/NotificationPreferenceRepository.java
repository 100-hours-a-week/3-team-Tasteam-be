package com.tasteam.domain.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	@Query("""
		SELECT p.memberId FROM MemberNotificationPreference p
		WHERE p.channel = :channel
		  AND p.notificationType = :type
		  AND p.isEnabled = false
		""")
	Set<Long> findDisabledMemberIds(
		@Param("channel")
		NotificationChannel channel,
		@Param("type")
		NotificationType type);
}
