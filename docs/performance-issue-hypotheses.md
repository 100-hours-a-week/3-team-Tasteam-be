# 동시성 30명 한계 - 원인 후보 정리

작성일: 2026-02-05

이 문서는 "동시 사용자 약 30명"에서 처리량이 급격히 떨어지는 현상의 원인 후보를 빠르게 추적하기 위한 목록입니다.
쿼리/트랜잭션/인프라/로깅 등 범주별로 가설을 정리하고, 검증 체크리스트를 포함합니다.

## 범위와 전제

- 성능 병목은 대부분 DB 쿼리/락/IO에서 발생할 가능성이 높음
- 다만 외부 API, 로깅 I/O, 커넥션 풀 설정 불일치도 병목을 유발 가능
- 본 문서는 **후보 리스트업**이며, 우선순위는 실측 지표로 조정

## High Priority 후보

1. **검색/추천/거리 기반 쿼리의 풀스캔 및 비싼 정렬**
   - 참조: `docs/query-optimization-candidates.md`
   - `LIKE %keyword%`, `order by random()`, `count + group by`, `ST_Distance` 반복 계산
   - 동시 요청 증가 시 DB CPU와 I/O를 급격히 소모

2. **PostGIS 지리 쿼리 인덱스 미적용 가능성**
   - `ST_DWithin`, `ST_Distance` 사용 구간 다수
   - GiST 인덱스 부재 시 공간 검색이 풀스캔으로 전환

3. **집계/정렬 기반 핫 리스트 경로**
   - `findHotRestaurants*`의 리뷰 카운트 정렬
   - 사전 집계 없으면 대규모 조인 + 정렬로 급격히 느려짐

4. **대량 `IN/NOT IN` 패턴**
   - `NOT IN (:excludeIds)`는 대량일수록 급격히 느려짐
   - NULL 포함 시 결과 이상 및 플랜 악화 가능

5. **읽기/쓰기 혼합 트랜잭션**
   - 검색 트랜잭션 내 히스토리 UPSERT 등
   - 불필요한 락/트랜잭션 유지로 동시성 한계 유발
   - 참조: `docs/troubleshooting/troubleshooting-search-transaction-isolation.md`

6. **서비스 레이어의 다중 쿼리 조합/N+1 가능성**
   - 서비스 비대화(`RestaurantService`, `GroupService`, `SubgroupService`)로 중복 조회 가능
   - 참조: `docs/refactoring-candidates.md`

7. **API/AOP 로깅 기본 활성화에 따른 I/O 병목**
   - `application.yml`에서 AOP 로깅 기본 `true`
   - 높은 QPS에서 로그 I/O가 병목이 될 수 있음

## Medium Priority 후보

1. **Hikari 풀 크기와 DB `max_connections` 불일치**
   - `application.prod.yml`의 `maximum-pool-size: 50`
   - DB 커넥션 제한/CPU 포화와 맞물려 동시성 상한 발생 가능

2. **커서 페이징 정렬 인덱스 미스**
   - `(deleted_at, updated_at desc, id desc)` 등 복합 인덱스 부재 시 정렬 비용 폭증

3. **검색 히스토리 UPSERT 실패 전파 경로 재존재 가능성**
   - 일부 경로에 트랜잭션 격리 미적용 시 동시 실패 확대

## Low Priority / 관찰 대상

1. **외부 API 호출 지연**
   - OAuth, Maps, Webhook 등 외부 I/O가 동기 경로에 있을 경우 병목

2. **파일/이미지 처리 경로**
   - 업로드/서명/검증 로직이 요청 경로에 있을 경우 응답 지연

## 즉시 확인 체크리스트

1. **슬로우 쿼리 상위 20개 추출**
   - `EXPLAIN (ANALYZE, BUFFERS)`로 플랜/시간 확인
   - 우선순위: `docs/query-optimization-candidates.md`의 High 항목

2. **DB 커넥션 대기 확인**
   - Hikari 활성 커넥션 수, 대기 시간
   - `pg_stat_activity`, 커넥션 풀 대기 로그

3. **락/장기 트랜잭션 확인**
   - `pg_locks` 및 장기 트랜잭션 세션 점검

4. **API별 응답 시간 분포**
   - 어떤 엔드포인트가 먼저 병목이 되는지 확인

## 다음 단계 제안

- 상위 3~5개 엔드포인트를 선정해 슬로우 쿼리/플랜 캡처
- 공간/검색/집계 쿼리의 인덱스 적용 계획 수립
- 읽기 전용 트랜잭션 분리 및 로깅 정책 튜닝
