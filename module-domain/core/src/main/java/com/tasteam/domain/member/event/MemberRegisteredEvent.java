package com.tasteam.domain.member.event;

import java.time.Instant;

public record MemberRegisteredEvent(
	Long memberId,
	String email,
	String nickname,
	Instant registeredAt) {
}
