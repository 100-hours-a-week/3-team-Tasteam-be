# 쿼리 최적화 후보 목록

이 문서는 현재 프로젝트에서 최적화가 필요하거나 개선 여지가 큰 쿼리들을 정리한 것입니다. 각 항목은 위치, 성능 위험 요인, 권장 개선 사항(인덱스 포함)을 포함합니다.

## High Priority 후보

1. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantRepository.java` `searchByKeyword`
이슈: `lower(...) like %:keyword%`는 일반 B-tree 인덱스를 사용하지 못함. 컬럼 OR 조건으로 스캔 범위 확대. 키셋 페이징 정렬(`updated_at`, `id`)에 대한 복합 인덱스 부재 가능.
권장: `pg_trgm` 또는 FTS 도입. 유지 시 `lower(name)`/`lower(full_address)` 함수 인덱스 추가. `(deleted_at, updated_at desc, id desc)` 복합 인덱스로 키셋 페이징 지원.

2. `app-api/src/main/java/com/tasteam/domain/search/repository/impl/SearchQueryRepositoryImpl.java` `searchRestaurantsByKeyword`
이슈: 위와 동일한 `lower(...) like %keyword%` 패턴으로 대량 스캔 가능.
권장: 위와 동일. 중복 구현을 줄이고 단일 검색 경로로 통합 권장.

3. `app-api/src/main/java/com/tasteam/domain/group/repository/impl/GroupQueryRepositoryImpl.java` `searchByKeyword`
이슈: `group.name.lower().like` 및 `group.address.lower().like`의 선행 와일드카드로 풀스캔 유발. OR 조건으로 범위 확대.
권장: `pg_trgm`/FTS 적용 또는 함수 인덱스. 검색 정확도 필요 시 `name`/`address` 분리 인덱스 고려.

4. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantRepository.java` `findRandomRestaurants`
이슈: `order by random()`은 O(N) 전체 정렬로 데이터 증가 시 급격히 느려짐.
권장: 랜덤 키 컬럼 사전 생성 + 인덱스, `TABLESAMPLE`, 또는 카운트 기반 랜덤 오프셋 등 대체 전략.

5. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantRepository.java` `findHotRestaurants`, `findHotRestaurantsAll`
이슈: `left join review` + `count` 정렬은 리뷰 테이블 대량 스캔 및 정렬 비용 큼. `group by r.id`로도 비용 크다.
권장: 리뷰 수 사전 집계(물리화 뷰/카운터). `review(restaurant_id, deleted_at)` 인덱스. 핫 경로에서는 `count` 정렬 회피.

6. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/impl/RestaurantQueryRepositoryImpl.java` `findRestaurantsWithDistance` (2개 오버로드)
이슈: `ST_Distance`를 한 쿼리에서 여러 번 계산(선택/커서 비교). 카테고리 조인 시 중복 레스토랑 가능. `geography`의 `ST_DWithin`은 GiST 인덱스 필수.
권장: LATERAL/CTE로 거리 1회 계산 후 정렬. 필요 시 `DISTINCT ON (r.id)` 또는 `GROUP BY`. `(location::geography)`에 GiST 인덱스 및 `<->` 연산자 고려.

7. `app-api/src/main/java/com/tasteam/domain/group/repository/GroupMemberRepository.java` `findMemberGroupDetailSummaries`
이슈: 멤버 수를 상관 서브쿼리로 row마다 계산하여 비용 증가.
권장: 조인+그룹 집계로 변경 또는 사전 집계 컬럼/테이블 사용. `group_member(group_id, deleted_at)` 인덱스 확인.

## Medium Priority 후보

1. `app-api/src/main/java/com/tasteam/domain/subgroup/repository/SubgroupRepository.java` `searchSubgroupsByGroup`, `findMySubgroupsByGroup`
이슈: `s.name like %:keyword%`로 풀스캔 유발. `memberCount` 정렬 + 커서 조건에 맞는 복합 인덱스 부재 가능.
권장: `subgroup.name`에 `pg_trgm` 인덱스. `(group_id, status, deleted_at, member_count desc, id asc)` 복합 인덱스 고려.

2. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantRepository.java` `findNearbyRestaurants`, `findNewRestaurants`
이슈: `ST_DWithin`/`ST_Distance` 중복 계산 및 GiST 인덱스 미존재 시 성능 저하. `created_at`/`id` 정렬에 맞는 인덱스 필요.
권장: `(location::geography)` GiST 인덱스. 거리 계산 1회로 통합. `(deleted_at, created_at desc, id desc)` 인덱스.

3. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantRepository.java` `findAiRecommendRestaurants`, `findAiRecommendRestaurantsAll`
이슈: `ai_restaurant_review_analysis` 조인 + `positive_review_ratio` 정렬은 풀 정렬 가능.
권장: `ai_restaurant_review_analysis(positive_review_ratio desc, restaurant_id)` 인덱스, `restaurant(deleted_at)` 인덱스.

4. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantRepository.java` `find*All` 계열의 `r.id not in (:excludeIds)`
이슈: 대량 `NOT IN`은 느리며 NULL 포함 시 결과가 꼬일 수 있음.
권장: `NOT EXISTS` + `unnest(:excludeIds)` 또는 임시 테이블. `excludeIds`는 널/크기 제한.

5. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantWeeklyScheduleRepository.java` `findEffectiveSchedules`
이슈: `effectiveFrom`/`effectiveTo`가 NULL 허용인 범위 조건은 인덱스 활용 어려움.
권장: 범위를 항상 채우는 방식으로 정규화. `(restaurant_id, day_of_week, effective_from, effective_to)` 인덱스.

6. `app-api/src/main/java/com/tasteam/domain/restaurant/repository/MenuRepository.java` `findByRestaurantIdOrderByNameWithCursor`
이슈: `(name, id)` 키셋 페이징은 복합 인덱스 없이 정렬 비용 발생.
권장: `(restaurant_id, name, id)` 복합 인덱스.

## Low Priority / 관찰 대상

1. `app-api/src/main/java/com/tasteam/domain/file/repository/DomainImageRepository.java` `findAllByDomainTypeAndDomainIdIn`
이슈: 대량 `IN` 리스트, 조인 fetch + 정렬로 메모리/네트워크 부담.
권장: `domainIds` 배치 처리. `(domain_type, domain_id, sort_order)` 인덱스. 필요 시 페이지네이션.

2. `app-api/src/main/java/com/tasteam/domain/review/repository/ReviewImageRepository.java`, `ReviewKeywordRepository.java`
이슈: `review.id in (:reviewIds)` 대량 리스트 및 정렬 비용.
권장: `reviewIds` 배치 처리. `review_image(review_id, deleted_at)`, `review_keyword(review_id, keyword_id)` 인덱스.

3. `app-api/src/main/java/com/tasteam/domain/search/repository/MemberSearchHistoryRepository.java` `upsertSearchHistory`
이슈: 업서트는 유니크 인덱스 필수. 누락 시 급격히 느려짐.
권장: `(member_id, keyword)` 유니크 인덱스. 소프트 삭제 사용 시 `deleted_at is null` 부분 인덱스 검토.

## Notes

- 대부분의 `LIKE %keyword%` 검색은 `pg_trgm`/FTS 인덱스 없이 확장성 한계가 큼.
- 지리 쿼리는 `EXPLAIN (ANALYZE, BUFFERS)`로 실제 플랜을 확인해야 함.
- 인덱스는 쓰기 부하/실제 조회 패턴을 고려해 단계적으로 도입할 것.



---


## 개선 로드맵 (1, 2, 6 우선)

1. 인덱스 DDL 초안 정리
범위: 아래 후보 쿼리들에 대해 구체적인 인덱스 DDL을 작성합니다. `IF NOT EXISTS` 포함, 롤백 메모, 각 인덱스의 필요성 요약을 함께 정리합니다. `deleted_at`, 키셋 페이징, 검색 필드, 지리 인덱스를 우선 대상으로 합니다.
산출물: 이 문서 내 섹션 추가 또는 별도 문서(예: `docs/query-index-ddl.md`)로 SQL 정리.

2. `EXPLAIN (ANALYZE, BUFFERS)` 검증 가이드
범위: 후보 쿼리별 검증 절차를 표준화합니다. 데이터 규모 가정, 플랜 캡처 방법, 수용 기준(예: `total time`, `rows`, `shared hit/read`)을 정의합니다.
산출물: 로컬/스테이징 환경 공통 체크리스트와 전후 측정 템플릿.

6. `deleted_at` 인덱스 전략 표준화
범위: 소프트 삭제(`deleted_at`)를 사용하는 테이블에 대해 일관된 복합 인덱스 정책을 수립합니다. (예: `(deleted_at, 정렬/조건 컬럼들)`)
산출물: 테이블별 목표 인덱스 목록과 롤링 배포 순서.

## 나머지 개선 작업 (1, 2, 6 이후)

3. 검색/추천 쿼리 구현 통합
목표: JPA/Querydsl/Native 혼재를 줄이고 하나의 주 경로로 통일하여 최적화 전략을 일관되게 적용.

4. 대량 `IN/NOT IN` 처리 규칙
목표: 대량 ID 리스트 처리 표준(배치 크기, `unnest`, 임시 테이블 활용)을 정의하고 코드에 반영.

5. 지리 쿼리 유틸리티화
목표: 거리 계산 및 `ST_DWithin` 사용을 공통화하여 중복 제거 및 인덱스 활용 일관성 확보.

7. 집계 쿼리 사전 계산
목표: 리뷰 수 등 고비용 집계는 materialized view 또는 비정규화 카운터 도입 검토.

8. 트랜잭션/락 정책 문서화
목표: `@Transactional`, 락 모드 사용 기준, 일관성 설정을 유형별로 정리.

9. 쿼리 로깅 정책 정비
목표: 개발/운영 환경 로깅 레벨, 샘플링, 슬로우 쿼리 기준 정리.