package com.tasteam.global.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 traceId / spanId를 MDC에 주입하는 필터.
 * X-Request-ID 헤더가 있으면 재사용, 없으면 UUID 신규 생성.
 * JSON 로그(logback-spring.xml)에 traceId·spanId 필드가 자동으로 포함됨.
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

	private static final String REQUEST_ID_HEADER = "X-Request-ID";
	private static final String MDC_TRACE_ID = "traceId";
	private static final String MDC_SPAN_ID = "spanId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {
		String incomingId = request.getHeader(REQUEST_ID_HEADER);
		String traceId = StringUtils.hasText(incomingId) ? incomingId : UUID.randomUUID().toString();
		String spanId = traceId.replace("-", "").substring(0, 8);

		try {
			MDC.put(MDC_TRACE_ID, traceId);
			MDC.put(MDC_SPAN_ID, spanId);
			response.addHeader(REQUEST_ID_HEADER, traceId);
			filterChain.doFilter(request, response);
		} finally {
			MDC.clear();
		}
	}
}
