# 쿼리 최적화 쉽게 설명

이 문서는 주요 쿼리들에 대해 "무슨 코드가 문제인지 → 내부 동작 → 개선 방법 → 개선 방식"을 쉽게 설명합니다.

- `RestaurantRepository.searchByKeyword`
- `SearchQueryRepositoryImpl.searchRestaurantsByKeyword`
- `GroupQueryRepositoryImpl.searchByKeyword`
- `RestaurantRepository.findRandomRestaurants`
- `RestaurantRepository.findHotRestaurants`, `findHotRestaurantsAll`
- `RestaurantQueryRepositoryImpl.findRestaurantsWithDistance`
- `GroupMemberRepository.findMemberGroupDetailSummaries`

## 1) RestaurantRepository.searchByKeyword

### 어떤 코드가 문제인가
- `lower(r.name) like %:keyword%`
- `lower(r.fullAddress) like %:keyword%`
- 두 조건을 `OR`로 결합

### 내부적으로 어떻게 동작하나
- `lower()` 같은 함수가 컬럼에 걸리면 일반 B‑tree 인덱스를 사용하기 어렵습니다.
- `LIKE '%키워드%'`는 앞에 와일드카드가 있어 “어느 위치에든” 매칭을 찾습니다. 이 형태는 인덱스 활용이 거의 불가능합니다.
- `name OR fullAddress`는 스캔 범위를 더 넓혀, 결국 테이블 전체를 읽는 계획이 선택될 가능성이 큽니다.
- 키셋 페이징(`updated_at`, `id`)을 하더라도 이를 받쳐줄 복합 인덱스가 없으면 정렬 비용이 증가합니다.

### 어떻게 개선하면 좋은가
1. `pg_trgm`(트라이그램) 인덱스 도입 또는 FTS(Full‑Text Search) 전환
2. 유지한다면 `lower(name)`, `lower(full_address)` 함수 인덱스 추가
3. 키셋 페이징용 복합 인덱스 `(deleted_at, updated_at desc, id desc)` 추가

### 개선이 어떤 방식으로 효과가 나는가
- `pg_trgm`/FTS는 부분 매칭(`%keyword%`)도 인덱스로 처리할 수 있게 합니다.
- 함수 인덱스는 `lower(col)` 조건을 인덱스에서 직접 처리할 수 있게 해줍니다.
- 키셋 페이징 인덱스는 정렬을 줄이고 필요한 범위만 빠르게 탐색합니다.

## 2) SearchQueryRepositoryImpl.searchRestaurantsByKeyword

### 어떤 코드가 문제인가
- `r.name.lower().like("%" + keyword + "%")`
- `r.fullAddress.lower().like("%" + keyword + "%")`

### 내부적으로 어떻게 동작하나
- 1번과 동일한 SQL 패턴이 생성되므로 인덱스 미사용 + 풀스캔 문제가 그대로 발생합니다.

### 어떻게 개선하면 좋은가
- 1번과 동일한 개선안을 적용합니다.
- 추가로 검색 구현을 한 곳으로 통합하면 최적화 관리가 쉬워집니다.

### 개선이 어떤 방식으로 효과가 나는가
- 1번과 동일한 원리로 인덱스 활용이 가능해지고 스캔 범위가 줄어듭니다.
- 구현 통합은 성능 정책 일관성을 보장합니다.

## 3) GroupQueryRepositoryImpl.searchByKeyword

### 어떤 코드가 문제인가
- `group.name.lower().like("%" + keyword + "%")`
- `group.address.lower().like("%" + keyword + "%")`
- `OR` 조건 결합

### 내부적으로 어떻게 동작하나
- `lower()` + 선행 와일드카드(`%keyword%`) 조합으로 인덱스가 거의 작동하지 않습니다.
- `OR` 조건 때문에 스캔 범위가 확대됩니다.
- 데이터가 늘수록 전체 스캔 비용이 급격히 커집니다.

### 어떻게 개선하면 좋은가
- `pg_trgm`/FTS 적용 또는 함수 인덱스 추가
- 검색 정확도가 중요하면 `name`/`address`를 분리 인덱스로 관리

### 개선이 어떤 방식으로 효과가 나는가
- 트라이그램/FTS는 부분 검색을 인덱스에서 처리합니다.
- 분리 인덱스는 특정 컬럼 검색에 더 효율적인 실행 계획을 선택하게 합니다.

## 4) RestaurantRepository.findRandomRestaurants

### 어떤 코드가 문제인가
- `order by random()` + `limit`

### 내부적으로 어떻게 동작하나
- 모든 후보 행에 대해 난수를 생성하고 이를 기준으로 정렬합니다.
- 정렬은 전 행을 대상으로 수행되므로 데이터가 늘수록 O(N log N) 비용이 커집니다.
- 인덱스를 거의 활용하지 못합니다.

### 어떻게 개선하면 좋은가
1. 랜덤 키 컬럼을 사전 생성하고 인덱스 추가
2. `TABLESAMPLE` 등 샘플링 방식 사용
3. 카운트 기반 랜덤 오프셋 방식 사용

### 개선이 어떤 방식으로 효과가 나는가
- 랜덤 키 컬럼 + 인덱스는 인덱스 스캔으로 빠르게 일부를 선택할 수 있습니다.
- 샘플링은 전체 정렬 없이 일부 블록만 읽습니다.
- 랜덤 오프셋은 전체 정렬을 피하지만 정확한 균등 분포는 보장하지 않습니다.

## 5) RestaurantRepository.findHotRestaurants, findHotRestaurantsAll

### 어떤 코드가 문제인가
- `left join review` + `count` + `order by count`
- `group by r.id`

### 내부적으로 어떻게 동작하나
- 리뷰 테이블과 조인 후 집계하므로 리뷰 수가 많을수록 스캔 비용이 증가합니다.
- `count`로 정렬하려면 집계 결과를 모두 만든 뒤 정렬해야 합니다.
- 결과적으로 큰 테이블에서 매우 무거운 쿼리가 됩니다.

### 어떻게 개선하면 좋은가
1. 리뷰 수 사전 집계(물리화 뷰 또는 카운터 컬럼)
2. `review(restaurant_id, deleted_at)` 인덱스 추가
3. 핫 경로에서는 `count` 정렬 대신 사전 집계 결과 사용

### 개선이 어떤 방식으로 효과가 나는가
- 사전 집계는 런타임 조인/집계를 제거해 응답 시간을 크게 단축합니다.
- 적절한 인덱스는 집계 갱신/조인 비용을 줄입니다.

## 6) RestaurantQueryRepositoryImpl.findRestaurantsWithDistance (2개 오버로드)

### 어떤 코드가 문제인가
- `ST_Distance`가 SELECT/커서 비교에서 여러 번 계산됨
- 카테고리 조인으로 중복 레스토랑 발생 가능
- `ST_DWithin`이 `geography`에 적용됨(인덱스 없으면 전체 스캔)

### 내부적으로 어떻게 동작하나
- `ST_Distance`는 계산 비용이 큰 함수로, 반복 호출 시 CPU 비용이 증가합니다.
- 카테고리 조인으로 같은 레스토랑이 여러 번 나타나면 정렬/중복 처리 비용이 커집니다.
- `ST_DWithin`은 GiST 인덱스가 없으면 거의 전체 스캔을 수행합니다.

### 어떻게 개선하면 좋은가
1. LATERAL/CTE로 거리 1회 계산 후 재사용
2. 중복 가능성이 있으면 `DISTINCT ON (r.id)` 또는 `GROUP BY` 적용
3. `(location::geography)` GiST 인덱스 추가
4. 가능하면 `<->` 연산자 기반 KNN 정렬 검토

### 개선이 어떤 방식으로 효과가 나는가
- 거리 계산 1회로 CPU 비용을 줄입니다.
- 중복 제거는 정렬/전송 비용을 줄입니다.
- GiST 인덱스는 공간 검색 범위를 빠르게 줄여 I/O를 감소시킵니다.

## 7) GroupMemberRepository.findMemberGroupDetailSummaries

### 어떤 코드가 문제인가
- 멤버 수를 상관 서브쿼리로 row마다 계산

### 내부적으로 어떻게 동작하나
- 각 그룹 행마다 서브쿼리가 실행되는 형태라, 데이터가 많아질수록 반복 비용이 커집니다.
- 집계를 한 번만 하면 될 일을 여러 번 수행합니다.

### 어떻게 개선하면 좋은가
1. 조인 + 그룹 집계로 변경
2. 사전 집계 컬럼/테이블 사용
3. `group_member(group_id, deleted_at)` 인덱스 확인

### 개선이 어떤 방식으로 효과가 나는가
- 한 번의 집계로 전체를 계산하므로 반복 비용이 제거됩니다.
- 인덱스는 집계 대상 범위를 줄여줍니다.

## Medium Priority 후보

## 8) SubgroupRepository.searchSubgroupsByGroup, findMySubgroupsByGroup

### 어떤 코드가 문제인가
- `s.name like %:keyword%` 검색
- `memberCount` 정렬 + 커서 조건

### 내부적으로 어떻게 동작하나
- 부분 검색은 인덱스를 타기 어렵고 풀스캔이 발생합니다.
- 정렬/커서에 맞는 복합 인덱스가 없으면 정렬 비용이 커집니다.

### 어떻게 개선하면 좋은가
1. `subgroup.name`에 `pg_trgm` 인덱스 추가
2. `(group_id, status, deleted_at, member_count desc, id asc)` 복합 인덱스 고려

### 개선이 어떤 방식으로 효과가 나는가
- 트라이그램 인덱스가 부분 검색을 인덱스로 처리합니다.
- 복합 인덱스가 정렬/페이징 비용을 줄입니다.

## 9) RestaurantRepository.findNearbyRestaurants, findNewRestaurants

### 어떤 코드가 문제인가
- `ST_DWithin`/`ST_Distance` 중복 계산
- `created_at`/`id` 정렬

### 내부적으로 어떻게 동작하나
- 공간 함수는 비용이 크고, 인덱스가 없으면 전체 스캔 가능성이 큽니다.
- 정렬용 인덱스가 없으면 정렬 비용이 커집니다.

### 어떻게 개선하면 좋은가
1. `(location::geography)` GiST 인덱스 추가
2. 거리 계산 1회로 통합
3. `(deleted_at, created_at desc, id desc)` 복합 인덱스 추가

### 개선이 어떤 방식으로 효과가 나는가
- 공간 인덱스가 후보 범위를 크게 줄입니다.
- 정렬 인덱스는 키셋 페이징을 빠르게 합니다.

## 10) RestaurantRepository.findAiRecommendRestaurants, findAiRecommendRestaurantsAll

### 어떤 코드가 문제인가
- `ai_restaurant_review_analysis` 조인 후 `positive_review_ratio` 정렬

### 내부적으로 어떻게 동작하나
- 정렬 인덱스가 없으면 풀 정렬이 발생합니다.
- 조인 비용이 크면 전체 쿼리 비용도 커집니다.

### 어떻게 개선하면 좋은가
1. `ai_restaurant_review_analysis(positive_review_ratio desc, restaurant_id)` 인덱스 추가
2. `restaurant(deleted_at)` 인덱스 확인

### 개선이 어떤 방식으로 효과가 나는가
- 정렬 인덱스로 Top-N 정렬 비용이 줄어듭니다.
- 조인 대상이 줄어들어 전체 비용이 감소합니다.

## 11) RestaurantRepository.find*All 계열의 `r.id not in (:excludeIds)`

### 어떤 코드가 문제인가
- 대량 `NOT IN` 리스트

### 내부적으로 어떻게 동작하나
- 리스트가 크면 비교 비용이 커지고 실행 계획이 불리해집니다.
- NULL이 포함되면 결과가 예상과 다르게 나올 수 있습니다.

### 어떻게 개선하면 좋은가
1. `NOT EXISTS` + `unnest(:excludeIds)` 방식
2. 임시 테이블 사용
3. `excludeIds` 크기 제한 및 NULL 제거

### 개선이 어떤 방식으로 효과가 나는가
- `NOT EXISTS`는 실행 계획이 더 안정적입니다.
- `unnest`/임시 테이블은 대량 리스트 비교를 효율화합니다.

## 12) RestaurantWeeklyScheduleRepository.findEffectiveSchedules

### 어떤 코드가 문제인가
- `effectiveFrom`/`effectiveTo`가 NULL 허용인 범위 조건

### 내부적으로 어떻게 동작하나
- NULL 포함 범위 조건은 인덱스 선택이 어려워집니다.

### 어떻게 개선하면 좋은가
1. 유효 기간을 항상 채우는 방식으로 정규화
2. `(restaurant_id, day_of_week, effective_from, effective_to)` 인덱스 추가

### 개선이 어떤 방식으로 효과가 나는가
- 범위 조건이 명확해져 인덱스를 더 잘 활용합니다.

## 13) MenuRepository.findByRestaurantIdOrderByNameWithCursor

### 어떤 코드가 문제인가
- `(name, id)` 키셋 페이징 정렬

### 내부적으로 어떻게 동작하나
- 복합 인덱스가 없으면 정렬 비용이 증가합니다.

### 어떻게 개선하면 좋은가
1. `(restaurant_id, name, id)` 복합 인덱스 추가

### 개선이 어떤 방식으로 효과가 나는가
- 정렬과 필터가 인덱스로 해결되어 페이지 탐색이 빨라집니다.

## 요약

- 검색 쿼리는 `lower()` + `LIKE '%키워드%'` + `OR` 조합으로 인덱스를 타지 못하는 점이 핵심 문제입니다.
- 랜덤 정렬, 대규모 집계/정렬, 공간 함수 반복 계산은 큰 테이블에서 성능 병목을 유발합니다.
- 트라이그램/FTS, 사전 집계, GiST 인덱스, 키셋 페이징 인덱스로 스캔과 정렬 비용을 줄일 수 있습니다.
