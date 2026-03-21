package com.tasteam.domain.chat.presence;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tasteam.infra.redis.RedisClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatRoomPresenceRegistry {

	private static final String ROOM_ONLINE_KEY_PREFIX = "chat:room:";
	private static final String SESSION_ROOMS_KEY_PREFIX = "chat:session:";
	private static final String SESSION_SUBS_KEY_SUFFIX = ":subs";
	private static final String SESSION_MEMBER_KEY_SUFFIX = ":member";
	private static final String SESSION_SUB_KEY_PREFIX = ":sub:";

	private final RedisClient redisClient;

	public void registerSubscription(String sessionId, String subscriptionId, Long memberId, Long chatRoomId) {
		if (sessionId == null || subscriptionId == null || memberId == null || chatRoomId == null) {
			return;
		}

		String roomKey = roomOnlineKey(chatRoomId);
		redisClient.sAdd(roomKey, memberId);

		String roomsKey = sessionRoomsKey(sessionId);
		redisClient.sAdd(roomsKey, chatRoomId);

		String subsKey = sessionSubsKey(sessionId);
		redisClient.sAdd(subsKey, subscriptionId);

		String subKey = sessionSubKey(sessionId, subscriptionId);
		redisClient.set(subKey, chatRoomId);

		String memberKey = sessionMemberKey(sessionId);
		redisClient.set(memberKey, memberId);
	}

	public void unregisterSubscription(String sessionId, String subscriptionId, Long memberId) {
		if (sessionId == null || subscriptionId == null) {
			return;
		}

		Long resolvedMemberId = memberId != null ? memberId : getSessionMemberId(sessionId);
		Long chatRoomId = getChatRoomIdForSubscription(sessionId, subscriptionId);
		if (resolvedMemberId != null && chatRoomId != null) {
			redisClient.sRemove(roomOnlineKey(chatRoomId), resolvedMemberId);
			redisClient.sRemove(sessionRoomsKey(sessionId), chatRoomId);
		}

		redisClient.sRemove(sessionSubsKey(sessionId), subscriptionId);
		redisClient.delete(sessionSubKey(sessionId, subscriptionId));
	}

	public void unregisterAll(String sessionId, Long memberId) {
		if (sessionId == null) {
			return;
		}
		Long resolvedMemberId = memberId != null ? memberId : getSessionMemberId(sessionId);
		if (resolvedMemberId != null) {
			Set<Object> roomIds = redisClient.sMembers(sessionRoomsKey(sessionId));
			for (Object roomId : roomIds) {
				redisClient.sRemove(roomOnlineKey(roomId), resolvedMemberId);
			}
		}

		Set<Object> subs = redisClient.sMembers(sessionSubsKey(sessionId));
		for (Object subId : subs) {
			redisClient.delete(sessionSubKey(sessionId, String.valueOf(subId)));
		}

		redisClient.delete(sessionRoomsKey(sessionId));
		redisClient.delete(sessionSubsKey(sessionId));
		redisClient.delete(sessionMemberKey(sessionId));
	}

	public boolean isMemberOnline(Long chatRoomId, Long memberId) {
		if (chatRoomId == null || memberId == null) {
			return false;
		}
		return redisClient.sIsMember(roomOnlineKey(chatRoomId), memberId);
	}

	public Long getSessionMemberId(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		return redisClient.get(sessionMemberKey(sessionId), Long.class).orElse(null);
	}

	private Long getChatRoomIdForSubscription(String sessionId, String subscriptionId) {
		return redisClient.get(sessionSubKey(sessionId, subscriptionId), Long.class).orElse(null);
	}

	private String roomOnlineKey(Object chatRoomId) {
		return ROOM_ONLINE_KEY_PREFIX + chatRoomId + ":online";
	}

	private String sessionRoomsKey(String sessionId) {
		return SESSION_ROOMS_KEY_PREFIX + sessionId + ":rooms";
	}

	private String sessionSubsKey(String sessionId) {
		return SESSION_ROOMS_KEY_PREFIX + sessionId + SESSION_SUBS_KEY_SUFFIX;
	}

	private String sessionMemberKey(String sessionId) {
		return SESSION_ROOMS_KEY_PREFIX + sessionId + SESSION_MEMBER_KEY_SUFFIX;
	}

	private String sessionSubKey(String sessionId, String subscriptionId) {
		return SESSION_ROOMS_KEY_PREFIX + sessionId + SESSION_SUB_KEY_PREFIX + subscriptionId;
	}
}
