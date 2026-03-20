package com.tasteam.infra.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface RedisClient {

	/**
	 * 키에 값을 저장합니다.
	 */
	void set(String key, Object value);

	/**
	 * 키에 값을 TTL과 함께 저장합니다.
	 */
	void set(String key, Object value, Duration ttl);

	/**
	 * 키가 존재하지 않을 경우에만 값을 저장합니다 (분산 락 구현에 유용).
	 */
	boolean setIfAbsent(String key, Object value);

	/**
	 * 키가 존재하지 않을 경우에만 값을 TTL과 함께 저장합니다.
	 */
	boolean setIfAbsent(String key, Object value, Duration ttl);

	/**
	 * 키에 저장된 값을 조회합니다.
	 */
	<T> Optional<T> get(String key, Class<T> type);

	/**
	 * 키를 삭제합니다.
	 */
	void delete(String key);

	/**
	 * 여러 키를 일괄 삭제합니다.
	 */
	void deleteAll(Collection<String> keys);

	/**
	 * 키의 존재 여부를 확인합니다.
	 */
	boolean exists(String key);

	/**
	 * 키에 만료 시간을 설정합니다.
	 */
	void expire(String key, Duration ttl);

	/**
	 * 키의 남은 만료 시간을 초 단위로 반환합니다.
	 */
	Long getExpire(String key);

	/**
	 * 패턴에 매칭되는 키를 검색합니다 (프로덕션 사용 주의).
	 */
	Set<String> keys(String pattern);

	/**
	 * Hash 자료구조에 필드-값 쌍을 설정합니다.
	 */
	void hSet(String key, String field, Object value);

	/**
	 * Hash 자료구조에 여러 필드-값 쌍을 일괄 설정합니다.
	 */
	void hSetAll(String key, Map<String, Object> map);

	/**
	 * Hash 자료구조에서 필드 값을 조회합니다.
	 */
	<T> Optional<T> hGet(String key, String field, Class<T> type);

	/**
	 * Hash 자료구조의 모든 필드-값 쌍을 조회합니다.
	 */
	Map<Object, Object> hGetAll(String key);

	/**
	 * Hash 자료구조에서 필드를 삭제합니다.
	 */
	void hDelete(String key, String... fields);

	/**
	 * Hash 자료구조에 필드가 존재하는지 확인합니다.
	 */
	boolean hExists(String key, String field);

	/**
	 * List 자료구조의 왼쪽(head)에 값을 추가합니다.
	 */
	void lPush(String key, Object value);

	/**
	 * List 자료구조의 오른쪽(tail)에 값을 추가합니다.
	 */
	void rPush(String key, Object value);

	/**
	 * List 자료구조의 왼쪽(head)에서 값을 제거하고 반환합니다.
	 */
	<T> Optional<T> lPop(String key, Class<T> type);

	/**
	 * List 자료구조의 오른쪽(tail)에서 값을 제거하고 반환합니다.
	 */
	<T> Optional<T> rPop(String key, Class<T> type);

	/**
	 * List 자료구조에서 지정된 범위의 요소를 조회합니다 (0부터 시작, -1은 끝).
	 */
	<T> List<T> lRange(String key, long start, long end, Class<T> type);

	/**
	 * List 자료구조의 크기를 반환합니다.
	 */
	Long lSize(String key);

	/**
	 * Set 자료구조에 값을 추가합니다.
	 */
	void sAdd(String key, Object... values);

	/**
	 * Set 자료구조의 모든 요소를 조회합니다.
	 */
	Set<Object> sMembers(String key);

	/**
	 * Set 자료구조에 값이 포함되어 있는지 확인합니다.
	 */
	boolean sIsMember(String key, Object value);

	/**
	 * Set 자료구조에서 값을 제거합니다.
	 */
	void sRemove(String key, Object... values);

	/**
	 * Set 자료구조의 크기를 반환합니다.
	 */
	Long sSize(String key);

	/**
	 * 키의 값을 1 증가시킵니다.
	 */
	Long increment(String key);

	/**
	 * 키의 값을 지정된 값만큼 증가시킵니다.
	 */
	Long increment(String key, long delta);

	/**
	 * 키의 값을 1 감소시킵니다.
	 */
	Long decrement(String key);

	/**
	 * 키의 값을 지정된 값만큼 감소시킵니다.
	 */
	Long decrement(String key, long delta);
}
