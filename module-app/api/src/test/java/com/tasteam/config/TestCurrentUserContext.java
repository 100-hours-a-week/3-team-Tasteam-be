package com.tasteam.config;

/**
 * ThreadLocal 기반으로 테스트 중 인증 사용자 ID를 관리하는 클래스
 * 통합 테스트에서 현재 사용자 컨텍스트를 명시적으로 제어하기 위해 사용한다.
 */
public class TestCurrentUserContext {

	private static final long DEFAULT_MEMBER_ID = 1L;

	private final ThreadLocal<Long> currentUserId = ThreadLocal.withInitial(() -> DEFAULT_MEMBER_ID);

	/**
	 * 현재 설정된 사용자 ID를 반환한다.
	 */
	public Long getCurrentUserId() {
		return currentUserId.get();
	}

	/**
	 * 테스트용 사용자 ID를 설정한다.
	 */
	public void setCurrentUserId(Long memberId) {
		currentUserId.set(memberId);
	}

	/**
	 * ThreadLocal 값을 제거하여 테스트 간 간섭을 방지한다.
	 */
	public void clear() {
		currentUserId.remove();
	}
}
