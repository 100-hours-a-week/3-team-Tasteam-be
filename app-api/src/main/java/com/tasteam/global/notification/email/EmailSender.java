package com.tasteam.global.notification.email;

import java.time.Instant;

public interface EmailSender {

	void sendGroupJoinVerification(String email, String code, Instant expiresAt);
}
