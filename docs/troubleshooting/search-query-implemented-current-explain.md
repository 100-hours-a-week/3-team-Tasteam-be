# 검색 쿼리 구현본 EXPLAIN 검증 (실제 적용 코드 기준)

## 1. 목적
- 검색 코드(`SearchQueryRepositoryImpl`)에 구현된 3개 전략을 DB 실행 계획으로 검증한다.
- “빠르다”만으로 판단하지 않고, 실제 병목 지점(스캔 방식, 정렬/조인 구조, JIT 부하, 필터 선별 효율)을 함께 판단한다.
- 이번 분석은 `Docker` 로드테스트 DB(`tasteam-db-loadtest`)에서 수행했다.

## 2. 분석 대상(현재 구현 그대로)
- 파일: `/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/app-api/src/main/java/com/tasteam/domain/search/repository/impl/SearchQueryRepositoryImpl.java`
- 전략 enum: `ONE_STEP`, `TWO_STEP`, `JOIN_AGGREGATE`
- 기본 설정: `TWO_STEP`, `candidate-limit=200` (`application.yml` 기준)
- 핵심 SQL 요소
  - 필터: `deleted_at IS NULL`, `lower(name) LIKE %kw% OR similarity(lower(name), kw) >= 0.3`
  - 거리: `ST_DWithin(location, point, radius)` + `ST_DistanceSphere`
  - 정렬 점수: `nameExact*100 + similarity*30 + distanceWeight*50`
  - 커서: `totalScore`, `updated_at`, `id` 비교 (`cursorCondition`)

## 3. 실행 조건
- DB: `tasteam-db-loadtest` (컨테이너 실행 중, DB 포트 `55432`)
- 실행 스크립트: `3-team-Tasteam-cloud/loadtest/suites/search-stress/explain-search-plan.sh`
- 실행일: `2026-03-10`
- 데이터 상태 확인:
  - `restaurant`: 10,000
  - `restaurant_food_category`: 15,000
  - `food_category`: 7
- 입력:
  - 키워드: `강남`, `치킨`, `더미식당-546d8cf5709d-홍대`
  - 반경: `RADIUS_M=3000`, `LATITUDE=33.153`, `LONGITUDE=126.037`
  - 페이지 크기: `10`
  - candidate limit: `200` (short 키워드), long 키워드는 스크립트 동작상 `80`으로 실행
- EXPLAIN 옵션(세션 GUC): 비용/버퍼/IO/JIT/병렬 관련 다수 ON
  - `ANALYZE, COSTS, VERBOSE, BUFFERS, WAL, TIMING, SUMMARY, SETTINGS`
  - `seq/random/join/hash/merge/nestloop/tid/bitmap/index/tid` 등 스캔·조인 옵션 ON
  - `work_mem=64MB`, `jit=on` 등

## 4. 사용 결과 파일
- 결과 로그: `/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-cloud/loadtest/results/search-explain/search-explain-20260310-111852.log`

## 5. 핵심 실행시간(실행시간 ms)
### 5.1 short keyword (`강남`, `치킨`) + candidate=200
| keyword | ONE_STEP OFF | ONE_STEP ON | TWO_STEP ON | JOIN_AGGREGATE ON |
|---|---:|---:|---:|---:|
| 강남 | 2280.689 | 1372.210 | 310.788 | 1117.755 |
| 치킨 | 652.127 | 235.203 | 340.426 | 845.224 |

전략별 평균( short 기준 )
- `ONE_STEP`: **1466.408 ms**
- `TWO_STEP`: **325.607 ms**
- `JOIN_AGGREGATE`: **981.489 ms**
- `ONE_STEP OFF` 대비 `ON`: 1372/652 케이스에서 ON이 빠르게 보임(거리/조건이 걸려 후보 수 감소)

### 5.2 long keyword (`더미식당-546d8cf5709d-홍대`) + candidate=80
- `ONE_STEP OFF`: 1249.369
- `ONE_STEP ON`: 1243.119
- `TWO_STEP ON`: 349.807
- `JOIN_AGGREGATE ON`: 948.649

## 6. 실행 계획 패턴(핵심만)
- `ONE_STEP OFF` (강남)
  - `Seq Scan` → `Sort` + `SubPlan` 형태
  - `SubPlan`은 카테고리 존재여부 체크를 위해 `restaurant_food_category`/`food_category` 해시조인으로 이어짐
  - 실제 인덱스 범위 탐색보다 전수 스캔 성격이 강함
- `ONE_STEP ON` (강남)
  - `Seq Scan`에 `st_dwithin` 필터가 붙는 형태
  - 거리 계산은 행 단위로 거의 전체 후보에 대해 평가됨 (`Seq Scan` 유지)
  - 카테고리 존재 체크는 `Nested Loop + Memoize + food_category_pkey` 구조로 진행
- `TWO_STEP ON` (강남)
- `Sort` 기반으로 `candidate` CTE(최대 200개)를 뽑고, 실제 조회는 `Nested Loop Semi Join`에서 후보 집합과 조인
  - `candidate` 단계 자체는 여전히 `restaurant` 전체 스캔 기반이므로, 대량 데이터에서는 스캔비용이 존재
  - 그 뒤 단계는 상대적으로 작아져 총 소요시간이 가장 낮게 나타남
- `JOIN_AGGREGATE ON` (강남)
  - `GroupAggregate + Nested Loop Left Join` + `Sort`
  - 카테고리 집계까지 한 번에 하려다 전체 집계 경로 비용이 커짐

## 7. 병목 해석
1. **색인 미사용이 기본 병목이다**
   - 핵심 필터/정렬이 `lower(name)`, `similarity`, 거리 계산 함수 조합이라 `restaurant`에 대한 전수 스캔 가능성이 높다.
   - 현재 쿼리에서 `name` 단일 인덱스만으론 절대적인 우위를 못 줌.
2. **`ONE_STEP`은 후보를 줄이기 전에 정렬 비용이 선행되어 전체 계산 부담이 크다**
   - 특히 OFF 조건에서 JIT 총합 시간이 매우 커서(약 1.8초) CPU 관점 부담 가능성이 큼.
3. **`TWO_STEP`이 현재 데이터/키워드에선 가장 효율적**
   - 후보 수를 먼저 제한하고, 실제 본 쿼리까지 이어지는 구조가 실측 응답시간 측면에서 유리.
4. **`JOIN_AGGREGATE`는 집계 연산 부하가 큼**
   - 카테고리 존재를 집계로 푸는 대신 `EXISTS`/최적화된 반정렬 경로를 선호할 여지가 있음.

## 8. 정리: “실제로 성능이 좋은가?”
- **현 구조에서 1회성 응답 기반 평가지표 기준은 `TWO_STEP`이 가장 유리**하다.
- 다만 지금 실험은 **단일 쿼리 실행(Cold/Hot 혼재)** 기준이므로, 운영 동시성에서는:
  - `JIT` 컴파일 오버드 비용과 CPU 사용률,
  - 동시 커넥션 시 `candidate` 단계의 `Seq Scan` 경쟁,
  - `work_mem/join` 자원 사용,
  을 별도로 확인해야 한다.

## 9. 다음 실험 제안(권장)
1. 동일 스크립트로 `candidate-limit`만 `40/80/200/400`으로 늘린 매트릭스 생성
2. `cursorCondition`이 있는 페이지2/이후 쿼리까지 동일 방식으로 실행(현재는 기본 페이지1 형태)
3. `jit=off`/`enable_seqscan=off` 비교 테스트로 색인 사용성/컴파일 오버드 분해
4. 실제 서비스에서 `search` 엔드포인트의 지표(429/502/timeout 포함)와 조합해 병목을 확정

## 10. 참고(문서 연계)
- 기존 비교 문서(2026-02-07): `/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/docs/troubleshooting/search-query-1step-2step-20260207.md`
- 검색 설정/전략: `/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/app-api/src/main/java/com/tasteam/domain/search/repository/SearchQueryProperties.java`
