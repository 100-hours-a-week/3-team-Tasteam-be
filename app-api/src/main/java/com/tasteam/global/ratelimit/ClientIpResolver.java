package com.tasteam.global.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class ClientIpResolver {

	public String resolve(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		// Temporary mode: trusted proxy validation is disabled.
		// Use the first X-Forwarded-For entry when present.
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xForwardedFor)) {
			String first = xForwardedFor.split(",")[0].trim();
			if (StringUtils.hasText(first)) {
				return first;
			}
		}
		return remoteAddr;
	}
}
