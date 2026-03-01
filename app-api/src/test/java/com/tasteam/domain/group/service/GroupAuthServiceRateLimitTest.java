package com.tasteam.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.event.GroupEventPublisher;
import com.tasteam.domain.group.repository.GroupAuthCodeRepository;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.group.type.GroupJoinType;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.config.DomainProperties;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.NotificationErrorCode;
import com.tasteam.global.notification.email.EmailSender;
import com.tasteam.global.ratelimit.RateLimitReason;
import com.tasteam.global.ratelimit.RateLimitResult;
import com.tasteam.global.ratelimit.RedisRateLimiter;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("GroupAuthService RateLimit 예외 매핑 단위 테스트")
class GroupAuthServiceRateLimitTest {

	private GroupAuthService newService(RedisRateLimiter redisRateLimiter) {
		return new GroupAuthService(
			mock(GroupAuthCodeRepository.class),
			mock(GroupMemberRepository.class),
			mock(MemberRepository.class),
			mock(PasswordEncoder.class),
			mock(EmailSender.class),
			mock(GroupEventPublisher.class),
			mock(GroupInviteTokenService.class),
			mock(DomainProperties.class),
			redisRateLimiter,
			new SimpleMeterRegistry());
	}

	private Group emailJoinGroup() {
		Group group = mock(Group.class);
		given(group.getJoinType()).willReturn(GroupJoinType.EMAIL);
		given(group.getEmailDomain()).willReturn("example.com");
		return group;
	}

	@Test
	@DisplayName("rate limiter unavailable이면 EMAIL_RATE_LIMITER_UNAVAILABLE을 던진다")
	void failClosed_unavailable_mapping() {
		RedisRateLimiter limiter = mock(RedisRateLimiter.class);
		given(limiter.checkMailSend(org.mockito.ArgumentMatchers.any()))
			.willReturn(new RateLimitResult(false, RateLimitReason.RATE_LIMITER_UNAVAILABLE, 0L));
		GroupAuthService service = newService(limiter);

		assertThatThrownBy(
			() -> service.sendGroupEmailVerification(emailJoinGroup(), 1L, "1.1.1.1", "user@example.com"))
			.isInstanceOfSatisfying(BusinessException.class,
				ex -> assertThat(ex.getErrorCode())
					.isEqualTo(NotificationErrorCode.EMAIL_RATE_LIMITER_UNAVAILABLE.name()));
	}

	@Test
	@DisplayName("24시간 block이면 EMAIL_BLOCKED_24H를 던진다")
	void blocked24h_mapping() {
		RedisRateLimiter limiter = mock(RedisRateLimiter.class);
		given(limiter.checkMailSend(org.mockito.ArgumentMatchers.any()))
			.willReturn(new RateLimitResult(false, RateLimitReason.EMAIL_BLOCKED_24H, 100L));
		GroupAuthService service = newService(limiter);

		assertThatThrownBy(
			() -> service.sendGroupEmailVerification(emailJoinGroup(), 1L, "1.1.1.1", "user@example.com"))
			.isInstanceOfSatisfying(BusinessException.class,
				ex -> assertThat(ex.getErrorCode()).isEqualTo(NotificationErrorCode.EMAIL_BLOCKED_24H.name()));
	}

	@Test
	@DisplayName("일반 limit 초과이면 EMAIL_RATE_LIMITED를 던진다")
	void rateLimited_mapping() {
		RedisRateLimiter limiter = mock(RedisRateLimiter.class);
		given(limiter.checkMailSend(org.mockito.ArgumentMatchers.any()))
			.willReturn(new RateLimitResult(false, RateLimitReason.RATE_LIMIT_IP_1M, 10L));
		GroupAuthService service = newService(limiter);

		assertThatThrownBy(
			() -> service.sendGroupEmailVerification(emailJoinGroup(), 1L, "1.1.1.1", "user@example.com"))
			.isInstanceOfSatisfying(BusinessException.class,
				ex -> assertThat(ex.getErrorCode()).isEqualTo(NotificationErrorCode.EMAIL_RATE_LIMITED.name()));
	}
}
