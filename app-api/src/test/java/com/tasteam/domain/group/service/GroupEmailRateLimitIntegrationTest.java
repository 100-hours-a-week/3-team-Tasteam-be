package com.tasteam.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.group.dto.GroupCreateRequest;
import com.tasteam.domain.group.dto.GroupCreateResponse;
import com.tasteam.domain.group.dto.GroupEmailVerificationResponse;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.fixture.GroupRequestFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.NotificationErrorCode;
import com.tasteam.global.ratelimit.RateLimitKeyFactory;
import com.tasteam.infra.email.EmailSender;

@ServiceIntegrationTest
@Transactional
@TestPropertySource(properties = {
	"NOTIFICATION_EMAIL_RATE_LIMIT_ENABLED=true",
	"NOTIFICATION_EMAIL_RATE_LIMIT_EMAIL_1M_LIMIT=1",
	"NOTIFICATION_EMAIL_RATE_LIMIT_IP_1M_LIMIT=5",
	"NOTIFICATION_EMAIL_RATE_LIMIT_USER_1M_LIMIT=5",
	"NOTIFICATION_EMAIL_RATE_LIMIT_EMAIL_1D_LIMIT=10",
	"NOTIFICATION_EMAIL_RATE_LIMIT_EMAIL_1M_TTL_SECONDS=1",
	"NOTIFICATION_EMAIL_RATE_LIMIT_IP_1M_TTL_SECONDS=1",
	"NOTIFICATION_EMAIL_RATE_LIMIT_USER_1M_TTL_SECONDS=1",
	"NOTIFICATION_EMAIL_RATE_LIMIT_BLOCK_TTL_SECONDS=2"
})
@DisplayName("그룹 이메일 발송 RateLimit 통합 테스트")
class GroupEmailRateLimitIntegrationTest {

	@Autowired
	private GroupFacade groupFacade;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private RateLimitKeyFactory keyFactory;

	@MockitoBean
	private EmailSender emailSender;

	private Group emailGroup;
	private List<Member> members;

	@BeforeEach
	void setUp() {
		redisTemplate.execute((RedisCallback<Void>)connection -> {
			connection.serverCommands().flushAll();
			return null;
		});

		GroupCreateRequest request = GroupRequestFixture.createEmailGroupRequest("레이트리밋그룹", "example.com");
		GroupCreateResponse response = groupFacade.createGroup(request);
		emailGroup = groupRepository.findById(response.id()).orElseThrow();

		members = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			members.add(memberRepository.save(MemberFixture.create("rate-user" + i + "@example.com", "회원" + i)));
		}
	}

	@Nested
	@DisplayName("1분 제한")
	class MinuteLimit {

		@Test
		@DisplayName("같은 email 1분 1회 제한을 초과하면 429를 반환한다")
		void email1m_exceeded_returns429() {
			groupFacade.sendGroupEmailVerification(emailGroup.getId(), members.get(0).getId(), "10.0.0.1",
				"same@example.com");

			assertTooManyRequests(
				() -> groupFacade.sendGroupEmailVerification(emailGroup.getId(), members.get(0).getId(), "10.0.0.1",
					"same@example.com"),
				NotificationErrorCode.EMAIL_RATE_LIMITED);
		}

		@Test
		@DisplayName("같은 IP에서 1분 5회를 초과하면 429를 반환한다")
		void ip1m_exceeded_returns429() {
			for (int i = 0; i < 5; i++) {
				groupFacade.sendGroupEmailVerification(
					emailGroup.getId(),
					members.get(i).getId(),
					"20.0.0.1",
					"ip-limit-" + i + "@example.com");
			}

			assertTooManyRequests(
				() -> groupFacade.sendGroupEmailVerification(
					emailGroup.getId(),
					members.get(5).getId(),
					"20.0.0.1",
					"ip-limit-6@example.com"),
				NotificationErrorCode.EMAIL_RATE_LIMITED);
		}

		@Test
		@DisplayName("같은 사용자 1분 5회를 초과하면 429를 반환한다")
		void user1m_exceeded_returns429() {
			Long memberId = members.get(0).getId();
			for (int i = 0; i < 5; i++) {
				groupFacade.sendGroupEmailVerification(
					emailGroup.getId(),
					memberId,
					"30.0.0." + i,
					"user-limit-" + i + "@example.com");
			}

			assertTooManyRequests(
				() -> groupFacade.sendGroupEmailVerification(
					emailGroup.getId(),
					memberId,
					"30.0.0.9",
					"user-limit-6@example.com"),
				NotificationErrorCode.EMAIL_RATE_LIMITED);
		}
	}

	@Nested
	@DisplayName("일일 제한/차단")
	class DailyLimit {

		@Test
		@DisplayName("11번째 일일 요청은 즉시 24h block + 429(EMAIL_BLOCKED_24H)")
		void emailDaily11th_blocked24h() {
			String email = "daily@example.com";
			String normalizedEmail = keyFactory.normalizeEmail(email);
			for (int i = 0; i < 10; i++) {
				groupFacade.sendGroupEmailVerification(emailGroup.getId(), null, "40.0.0." + i, email);
				redisTemplate.delete(keyFactory.email1mKey(normalizedEmail));
			}

			assertTooManyRequests(
				() -> groupFacade.sendGroupEmailVerification(emailGroup.getId(), null, "40.0.0.99", email),
				NotificationErrorCode.EMAIL_BLOCKED_24H);
			assertThat(redisTemplate.hasKey(keyFactory.emailBlockKey(normalizedEmail))).isTrue();
		}

		@Test
		@DisplayName("block 상태에서는 추가 요청이 항상 EMAIL_BLOCKED_24H")
		void blockedEmail_alwaysBlocked() {
			String email = "blocked@example.com";
			String normalizedEmail = keyFactory.normalizeEmail(email);
			for (int i = 0; i < 10; i++) {
				groupFacade.sendGroupEmailVerification(emailGroup.getId(), null, "50.0.0." + i, email);
				redisTemplate.delete(keyFactory.email1mKey(normalizedEmail));
			}
			assertTooManyRequests(
				() -> groupFacade.sendGroupEmailVerification(emailGroup.getId(), null, "50.0.0.11", email),
				NotificationErrorCode.EMAIL_BLOCKED_24H);

			assertTooManyRequests(
				() -> groupFacade.sendGroupEmailVerification(emailGroup.getId(), members.get(0).getId(), "51.0.0.1",
					email),
				NotificationErrorCode.EMAIL_BLOCKED_24H);
		}
	}

	@Test
	@DisplayName("TTL 만료 후에는 다시 정상 요청이 가능하다")
	void ttlExpiry_restoresNormalBehavior() throws InterruptedException {
		String email = "ttl@example.com";
		Long memberId = members.get(0).getId();
		groupFacade.sendGroupEmailVerification(emailGroup.getId(), memberId, "60.0.0.1", email);

		assertTooManyRequests(
			() -> groupFacade.sendGroupEmailVerification(emailGroup.getId(), memberId, "60.0.0.1", email),
			NotificationErrorCode.EMAIL_RATE_LIMITED);

		Thread.sleep(1200);
		GroupEmailVerificationResponse response = groupFacade.sendGroupEmailVerification(
			emailGroup.getId(), memberId, "60.0.0.1", email);
		assertThat(response.expiresAt()).isAfter(Instant.now());
	}

	private void assertTooManyRequests(ThrowingAction action, NotificationErrorCode expectedCode) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(BusinessException.class, ex -> {
				assertThat(ex.getHttpStatus().value()).isEqualTo(429);
				assertThat(ex.getErrorCode()).isEqualTo(expectedCode.name());
			});
	}

	@FunctionalInterface
	private interface ThrowingAction {
		void run();
	}
}
