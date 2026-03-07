package com.tasteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tasteam.config.annotation.PerformanceTest;
import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.auth.repository.RefreshTokenRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;

@PerformanceTest
@DisplayName("[성능] TokenRefreshService 동시성 테스트")
class TokenRefreshServiceConcurrencyPerformanceTest {

	private static final int THREAD_COUNT = 20;

	@Autowired
	private TokenRefreshService tokenRefreshService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private RefreshTokenStore refreshTokenStore;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@AfterEach
	void tearDown() {
		refreshTokenRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("동일 리프레시 토큰으로 여러 스레드가 동시에 갱신하면 모두 성공한다")
	void refreshTokens_withSameTokenConcurrently() throws Exception {
		Member member = memberRepository.save(MemberFixture.create("perf-same@example.com", "성능_동일"));
		String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());
		saveRefreshToken(member.getId(), refreshToken);

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		List<Callable<Boolean>> tasks = new ArrayList<>();
		AtomicInteger successCount = new AtomicInteger(0);

		try {
			for (int i = 0; i < THREAD_COUNT; i++) {
				tasks.add(() -> {
					ready.countDown();
					start.await();
					TokenRefreshService.TokenPair tokenPair = tokenRefreshService.refreshTokens(refreshToken);
					if (!tokenPair.accessToken().isBlank()) {
						successCount.incrementAndGet();
						return true;
					}
					return false;
				});
			}

			List<Future<Boolean>> futures = new ArrayList<>();
			for (Callable<Boolean> task : tasks) {
				futures.add(executor.submit(task));
			}

			ready.await();
			start.countDown();

			for (Future<Boolean> future : futures) {
				assertThat(future.get(10, TimeUnit.SECONDS)).isTrue();
			}
		} finally {
			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
		}

		assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
		assertThat(refreshTokenStore.findByTokenHash(RefreshTokenHasher.hash(refreshToken))).isPresent();
	}

	@Test
	@DisplayName("각기 다른 리프레시 토큰을 가진 사용자들이 동시에 갱신 요청해도 모두 성공한다")
	void refreshTokens_withDifferentTokensConcurrently() throws Exception {
		List<Member> members = new ArrayList<>();
		List<String> refreshTokens = new ArrayList<>();

		for (int i = 0; i < THREAD_COUNT; i++) {
			Member member = memberRepository.save(MemberFixture.create("perf-user-" + i + "@example.com", "사용자" + i));
			members.add(member);
			String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());
			saveRefreshToken(member.getId(), refreshToken);
			refreshTokens.add(refreshToken);
		}

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger(0);
		List<Callable<Boolean>> tasks = new ArrayList<>();

		for (int i = 0; i < THREAD_COUNT; i++) {
			int index = i;
			tasks.add(() -> {
				ready.countDown();
				start.await();
				TokenRefreshService.TokenPair tokenPair = tokenRefreshService.refreshTokens(refreshTokens.get(index));
				if (!tokenPair.accessToken().isBlank()) {
					successCount.incrementAndGet();
					return true;
				}
				return false;
			});
		}

		try {
			List<Future<Boolean>> futures = new ArrayList<>();
			for (Callable<Boolean> task : tasks) {
				futures.add(executor.submit(task));
			}
			ready.await();
			start.countDown();

			for (Future<Boolean> future : futures) {
				assertThat(future.get(10, TimeUnit.SECONDS)).isTrue();
			}
		} finally {
			executor.shutdown();
			executor.awaitTermination(10, TimeUnit.SECONDS);
		}

		assertThat(successCount.get()).isEqualTo(THREAD_COUNT);
		assertThat(members).hasSize(THREAD_COUNT);
		assertThat(refreshTokenRepository.count()).isEqualTo(THREAD_COUNT);
	}

	private void saveRefreshToken(Long memberId, String refreshToken) {
		RefreshToken stored = RefreshToken.issue(
			memberId,
			RefreshTokenHasher.hash(refreshToken),
			"token-family-" + memberId,
			jwtTokenProvider.getExpiration(refreshToken).toInstant());
		refreshTokenStore.save(stored);
	}
}
