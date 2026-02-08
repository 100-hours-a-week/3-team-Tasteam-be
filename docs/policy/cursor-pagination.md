# Cursor 기반 페이지네이션 정책 (CursorPageBuilder)

## 목적
- 커서 디코딩/유효성 검사, `size+1` 조회 결과에서 `hasNext` 계산, `nextCursor` 생성까지 반복되는 로직을 표준화해 서비스 코드 중복과 실수를 줄인다.
- 잘못된 커서 입력 시 처리 정책을 명시적으로 선택할 수 있게 한다.

## 핵심 컴포넌트
- `CursorCodec` : 커서 객체 <-> Base64(JSON) 직렬화/역직렬화.
- `CursorPageBuilder<C>` : 커서 처리 상위 헬퍼.
  - `of(cursorCodec, rawCursor, CursorType.class)` : 커서 디코드 + 유효성 플래그 생성.
  - `build(items, requestedSize, lastItem -> new CursorType(...))` :
    - 잘못된 커서면 `Page.empty()` 반환(빈 리스트, `hasNext=false`).
    - `requestedSize+1`로 조회한 리스트를 받아 `hasNext` 계산 후 슬라이싱.
    - 마지막 아이템으로 `nextCursor` 생성.
    - 반환 `Page<T>(items, nextCursor, hasNext, requestedSize, size)` (`size`는 실제 응답 개수).
  - `buildOrThrow(..., invalidCursorExceptionSupplier)` : 커서가 잘못되면 명시적 예외로 처리하고 싶을 때 사용.
  - `fetchSize(requestedSize)` : 리포지토리 조회 시 사용할 `size+1` 계산 헬퍼.

## 사용 예시 (서비스)
```java
int requestedSize = resolveSize(sizeParam);
CursorPageBuilder<SubgroupNameCursor> pageBuilder =
    CursorPageBuilder.of(cursorCodec, rawCursor, SubgroupNameCursor.class);

List<SubgroupListItem> items = subgroupRepository.findSubgroupsByGroup(
    groupId,
    SubgroupStatus.ACTIVE,
    pageBuilder.cursor() == null ? null : pageBuilder.cursor().name(),
    pageBuilder.cursor() == null ? null : pageBuilder.cursor().id(),
    PageRequest.of(0, CursorPageBuilder.fetchSize(requestedSize)));

CursorPageBuilder.Page<SubgroupListItem> page = pageBuilder.build(
    items,
    requestedSize,
    last -> new SubgroupNameCursor(last.getName(), last.getSubgroupId()));
```

## 잘못된 커서 처리
- 기본(`build`) : 빈 페이지 반환 → 클라이언트는 더 이상 다음 페이지가 없다고 해석.
- 명시적 오류 필요 시 : `buildOrThrow(..., () -> new BusinessException(CommonErrorCode.INVALID_REQUEST))`

## 응답 필드 정렬
- `requestedSize` : 클라이언트가 요청한 사이즈(정책/로그 등에 활용).
- `size` : 실제 응답 아이템 수.
- `hasNext` : `requestedSize+1` 조회 여부로 계산.
- `nextCursor` : `hasNext`가 true일 때만 생성.

## 도입/확대 가이드
1) 커서 DTO 유지: 기존 커서 타입을 그대로 사용해 `CursorPageBuilder.of(...)`로 교체한다.
2) 리포지토리 조회 크기: `CursorPageBuilder.fetchSize(requestedSize)`로 통일한다.
3) 응답 DTO 매핑 시 `requestedSize`와 실제 `size`를 구분해 넣는다.
4) 잘못된 커서 정책을 서비스 성격에 따라 `build`(무음 처리) 또는 `buildOrThrow`(예외)로 선택한다.

## 현재 적용 범위
- `SubgroupService`에 우선 적용됨. 다른 커서 기반 서비스(예: Restaurant, Group 등)도 동일 패턴으로 점진 적용 예정.
