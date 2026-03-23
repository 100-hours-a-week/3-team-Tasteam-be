package com.tasteam.global.utils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * curor 기반의 페이징을 위한 공통 헬퍼
 * - 디코딩 및 유효성 검사 (유효하지 않은 커서 -> 빈 페이지 반환)
 * - size + 1 쿼리 패턴을 이용한 hasNext 계산
 * - 현재 페이지의 마지막 아이템으로부터 다음 커서 인코딩
 */
public class CursorPageBuilder<C> {

	private final CursorCodec cursorCodec;
	private final C cursor;
	private final boolean invalidCursor;

	private CursorPageBuilder(CursorCodec cursorCodec, C cursor, boolean invalidCursor) {
		this.cursorCodec = cursorCodec;
		this.cursor = cursor;
		this.invalidCursor = invalidCursor;
	}

	public static <C> CursorPageBuilder<C> of(CursorCodec cursorCodec, String rawCursor, Class<C> cursorType) {
		C decoded = cursorCodec.decodeOrNull(rawCursor, cursorType);
		boolean invalid = rawCursor != null && !rawCursor.isBlank() && decoded == null;
		return new CursorPageBuilder<>(cursorCodec, decoded, invalid);
	}

	public C cursor() {
		return cursor;
	}

	public boolean isInvalid() {
		return invalidCursor;
	}

	public <T> Page<T> build(List<T> queriedItems, int requestedSize, Function<T, C> nextCursorMapper) {
		if (requestedSize < 0) {
			throw new IllegalArgumentException("요청된 페이지 크기는 0 이상이어야 합니다");
		}
		if (invalidCursor) {
			return Page.empty();
		}

		boolean hasNext = queriedItems.size() > requestedSize;
		List<T> pageItems = hasNext ? queriedItems.subList(0, requestedSize) : queriedItems;

		String nextCursor = null;
		if (hasNext && !pageItems.isEmpty()) {
			T lastItem = pageItems.get(pageItems.size() - 1);
			nextCursor = cursorCodec.encode(nextCursorMapper.apply(lastItem));
		}

		return new Page<>(List.copyOf(pageItems), nextCursor, hasNext, requestedSize, pageItems.size());
	}

	/**
	 * 커스텀 invalid-cursor 정책으로 빌드합니다.
	 * 만약 커서가 유효하지 않다면, {@code invalidCursorException}에 의해 공급된 예외를 던집니다.
	 */
	public <T, E extends RuntimeException> Page<T> buildOrThrow(List<T> queriedItems, int requestedSize,
		Function<T, C> nextCursorMapper, Supplier<E> invalidCursorException) {
		if (invalidCursor) {
			throw invalidCursorException.get();
		}
		return build(queriedItems, requestedSize, nextCursorMapper);
	}

	/**
	 * 다음 페이지 조회를 위한 fetch size를 반환합니다.
	 */
	public static int fetchSize(int requestedSize) {
		if (requestedSize < 0) {
			throw new IllegalArgumentException("요청된 페이지 크기는 0 이상이어야 합니다");
		}
		return requestedSize + 1;
	}

	public record Page<T>(List<T> items, String nextCursor, boolean hasNext, int requestedSize, int size) {
		public static <T> Page<T> empty() {
			return new Page<>(List.of(), null, false, 0, 0);
		}
	}
}
