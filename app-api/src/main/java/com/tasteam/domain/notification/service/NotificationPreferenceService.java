package com.tasteam.domain.notification.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.notification.dto.request.NotificationPreferenceUpdateRequest;
import com.tasteam.domain.notification.dto.request.PushNotificationTargetRegisterRequest;
import com.tasteam.domain.notification.dto.response.NotificationPreferenceResponse;
import com.tasteam.domain.notification.entity.MemberNotificationPreference;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.entity.PushNotificationTarget;
import com.tasteam.domain.notification.repository.NotificationPreferenceRepository;
import com.tasteam.domain.notification.repository.PushNotificationTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

	private final NotificationPreferenceRepository preferenceRepository;
	private final PushNotificationTargetRepository pushTargetRepository;

	@Transactional(readOnly = true)
	public List<NotificationPreferenceResponse> getPreferences(Long memberId) {
		List<MemberNotificationPreference> savedPreferences = preferenceRepository.findAllByMemberId(memberId);

		Map<String, MemberNotificationPreference> savedMap = savedPreferences.stream()
			.collect(Collectors.toMap(
				p -> p.getChannel().name() + "_" + p.getNotificationType().name(),
				p -> p));

		List<NotificationPreferenceResponse> result = new ArrayList<>();
		for (NotificationChannel channel : NotificationChannel.values()) {
			for (NotificationType type : NotificationType.values()) {
				String key = channel.name() + "_" + type.name();
				MemberNotificationPreference saved = savedMap.get(key);
				if (saved != null) {
					result.add(NotificationPreferenceResponse.from(saved));
				} else {
					result.add(NotificationPreferenceResponse.ofDefault(channel, type));
				}
			}
		}
		return result;
	}

	@Transactional
	public void updatePreferences(Long memberId, NotificationPreferenceUpdateRequest request) {
		for (NotificationPreferenceUpdateRequest.NotificationPreferenceItem item : request.notificationPreferences()) {
			preferenceRepository.findByMemberIdAndChannelAndNotificationType(
				memberId, item.channel(), item.notificationType())
				.ifPresentOrElse(
					existing -> existing.changeEnabled(item.isEnabled()),
					() -> preferenceRepository.save(
						MemberNotificationPreference.create(memberId, item.channel(), item.notificationType(),
							item.isEnabled())));
		}
	}

	@Transactional
	public void registerPushTarget(Long memberId, PushNotificationTargetRegisterRequest request) {
		String deviceId = request.deviceId();
		String fcmToken = request.fcmToken();

		pushTargetRepository.findByFcmToken(fcmToken)
			.filter(existing -> !memberId.equals(existing.getMemberId())
				|| (existing.getDeviceId() != null && !existing.getDeviceId().equals(deviceId)))
			.ifPresent(pushTargetRepository::delete);

		pushTargetRepository.findByMemberIdAndDeviceId(memberId, deviceId)
			.ifPresentOrElse(
				existing -> existing.changeFcmToken(fcmToken),
				() -> pushTargetRepository.save(PushNotificationTarget.create(memberId, deviceId, fcmToken)));
	}
}
