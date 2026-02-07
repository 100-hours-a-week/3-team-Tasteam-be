# 검색 API 커넥션 풀 고갈 (REQUIRES_NEW로 2배 커넥션 점유)

## 증상
- 2026-02-05 15:20:50 KST, Production `/api/v1/search` 전 요청 실패(429/504).
- Hikari 로그: `Connection is not available ... (total=50, active=50, idle=0, waiting=199)` – 풀 고갈.
- k6 부하 테스트(500 VU): `http_req_failed 100%`, `p95=5s`.

## 원인
- `SearchService.search()`가 `@Transactional(readOnly = true)`로 커넥션 1개 점유.
- 내부 `SearchHistoryRecorder.recordSearchHistory()`가 `@Transactional(REQUIRES_NEW)` → **추가 커넥션 1개**를 즉시 점유.
- 결과: 한 요청이 동시에 2커넥션을 사용 → 풀 50개 기준 실효 동시성 25, 큐 대기 급증.
- 일부 동시 검색 시 중복 INSERT 경합으로 커넥션 점유 시간이 더 길어짐.

## 조치(2026-02-07)
1) **비동기화로 커넥션 1개로 축소**
   - `SearchHistoryRecorder`: `@Async("searchHistoryExecutor")` + 기본 `@Transactional`, REQUIRES_NEW 제거.
   - 전용 스레드풀 `searchHistoryExecutor` 추가 (core 5 / max 10 / queue 100, prefix `search-history-`).
2) **DB 유니크 인덱스 추가**
   - `V20260207__add_unique_index_member_search_history.sql`
   - 인덱스: `(member_id, keyword, deleted_at)`로 중복 INSERT/경합 방지.
3) **타임아웃 단축**
   - `hikari.connection-timeout` dev/prod/local 30s/20s → **10s**로 감소(고갈 시 빠른 fail).
4) **관측 강화**
   - 모든 프로필에서 `hibernate.generate_statistics` ON.
   - AOP `transactional-query-logging` 활성화(dev/prod/test/local)로 @Transactional별 쿼리 수/시간 로깅.

## 적용 파일
- `app-api/src/main/java/com/tasteam/domain/search/service/SearchHistoryRecorder.java`
- `app-api/src/main/java/com/tasteam/global/config/AsyncConfig.java`
- `app-api/src/main/resources/db/migration/V20260207__add_unique_index_member_search_history.sql`
- `app-api/src/main/resources/application.{dev,prod,local}.yml`
- `app-api/src/test/resources/application-test.yml`

## 효과 기대치
- 요청당 커넥션 점유 2 → 1 감소 → 동시 처리량 2배.
- 중복 INSERT로 인한 경합/예외 감소.
- 풀 고갈 시 10초 내 빠른 실패로 큐 적체 완화.

## 재발 방지 체크리스트
- REQUIRES_NEW 사용 시 커넥션 2배 점유 여부 검토.
- 비동기/이벤트 기반으로 부수 로직 분리 우선.
- 인기 키워드 캐시 스탬피드 방어(single-flight/예열) 검토.
- 부하 테스트 사전 수행: 500 VU, p95 < 500ms, timeout/429 < 0.5%, pending=0 확인.
