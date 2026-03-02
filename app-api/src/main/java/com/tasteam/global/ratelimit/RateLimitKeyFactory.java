package com.tasteam.global.ratelimit;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitKeyFactory {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private final RateLimitPolicy policy;

	public String normalizeEmail(String email) {
		if (!StringUtils.hasText(email)) {
			return "";
		}
		return email.trim()
			.toLowerCase(Locale.ROOT)
			.replaceAll("\\s+", "");
	}

	public String normalizeIp(String ip) {
		if (!StringUtils.hasText(ip)) {
			return "unknown";
		}
		return ip.trim();
	}

	public String email1mKey(String normalizedEmail) {
		return policy.getPrefix() + ":" + policy.getAction() + ":email:1m:" + normalizedEmail;
	}

	public String ip1mKey(String normalizedIp) {
		return policy.getPrefix() + ":" + policy.getAction() + ":ip:1m:" + normalizedIp;
	}

	public String user1mKey(Long userId) {
		if (userId == null) {
			return "";
		}
		return policy.getPrefix() + ":" + policy.getAction() + ":user:1m:" + userId;
	}

	public String email1dKey(String normalizedEmail, ZonedDateTime now) {
		LocalDate localDate = now.withZoneSameInstant(policy.zoneId()).toLocalDate();
		return policy.getPrefix() + ":" + policy.getAction() + ":email:1d:"
			+ normalizedEmail + ":" + DATE_FORMAT.format(localDate);
	}

	public String emailBlockKey(String normalizedEmail) {
		return policy.getPrefix() + ":block:email:" + normalizedEmail;
	}
}
