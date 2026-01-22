package com.tasteam.global.notification.email;

import java.time.Instant;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

	@Override
	public void sendGroupJoinVerification(String email, String code, Instant expiresAt) {
		log.info("Email verification code sent. email={}, code={}, expiresAt={}", email, code, expiresAt);
	}
}
