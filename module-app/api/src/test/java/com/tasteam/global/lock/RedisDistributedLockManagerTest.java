package com.tasteam.global.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Lock) RedisDistributedLockManager 단위 테스트")
class RedisDistributedLockManagerTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Test
	@DisplayName("락 획득에 성공하면 handle을 반환하고 close 시 release 스크립트를 실행한다")
	void tryLock_success_returnsHandleAndReleasesOnClose() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);

		RedisDistributedLockManager manager = new RedisDistributedLockManager(redisTemplate);

		var handleOpt = manager.tryLock("lock:test", Duration.ofSeconds(30));

		assertThat(handleOpt).isPresent();
		handleOpt.get().close();

		then(redisTemplate).should().execute(any(), anyList(), anyString());
	}

	@Test
	@DisplayName("락 획득에 실패하면 빈 Optional을 반환한다")
	void tryLock_failure_returnsEmpty() {
		given(redisTemplate.opsForValue()).willReturn(valueOperations);
		given(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(false);

		RedisDistributedLockManager manager = new RedisDistributedLockManager(redisTemplate);

		var handleOpt = manager.tryLock("lock:test", Duration.ofSeconds(30));

		assertThat(handleOpt).isEmpty();
		then(redisTemplate).shouldHaveNoMoreInteractions();
	}
}
