# Loadtest Seed

`loadtest/seed/`는 부하테스트 전에 필요한 더미 데이터 생성과 주입 기준을 관리합니다.
현재 표준 기준은 `generate_dummy_seed_sql.py + default_seed_profile.json` 조합 하나입니다.

## 표준 기준

- 진입점은 `generate_dummy_seed_sql.py` 하나만 사용합니다.
- 기본 프리셋은 `default_seed_profile.json`입니다.
- 생성 결과물은 항상 `loadtest/results/generated-seed/...` 아래에만 둡니다.
- 저장소 안에 generated SQL 예시 파일은 두지 않습니다.
- `join_type=PASSWORD` 그룹의 `group_auth_code`도 함께 생성하며, 기본 가입 코드는 `1234`입니다.
- `group_auth_code.code` 값은 평문이 아니라 Spring `BCryptPasswordEncoder` 기준 bcrypt 해시값으로 들어갑니다.

## 빠른 시작

기본 시드 생성:

```bash
cd loadtest/seed
python3 generate_dummy_seed_sql.py
```

기본 출력 파일:

- `loadtest/results/generated-seed/latest/seed.sql`
- `loadtest/results/generated-seed/latest/cleanup.sql`

DB 주입:

```bash
cd loadtest/seed
psql "$DATABASE_URL" -f ../results/generated-seed/latest/seed.sql
```

정리:

```bash
cd loadtest/seed
psql "$DATABASE_URL" -f ../results/generated-seed/latest/cleanup.sql
```

설정 미리보기:

```bash
cd loadtest/seed
python3 generate_dummy_seed_sql.py --print-config
```

## 기본 프리셋 개요

`default_seed_profile.json`은 실서비스처럼 보이는 데이터 분포를 유지하면서도 성능 검증에 필요한 볼륨을 확보하는 용도입니다.

주요 기본값:

- `member_count`: 10,000
- `restaurant_count`: 10,000
- `group_count`: 1,000
- `subgroup_per_group`: 4
- `review_count`: 300,000
- `chat_message_per_room`: 167
- `notification_count`: 100,000
- `favorite_count`: 100,000
- `subgroup_favorite_count`: 12,000
- `announcement_count`: 60
- `promotion_count`: 120
- `report_count`: 50,000
- `include_async_log_data`: `true`
- `async_log_scale_percent`: `30`
- `realistic_names`: `true`
- `seoul_focused_coords`: `true`

## 생성되는 데이터

핵심 도메인 데이터:

- `member`, `member_oauth_account`, `member_notification_preference`, `member_search_history`
- `group`, `group_auth_code`, `group_member`
- `subgroup`, `subgroup_member`, `subgroup_favorite_restaurant`
- `chat_room`, `chat_room_member`, `chat_message`, `chat_message_file`
- `restaurant`, `restaurant_address`, `restaurant_weekly_schedule`, `restaurant_schedule_override`
- `menu_category`, `menu`, `restaurant_food_category`
- `review`, `review_keyword`, `restaurant_review_summary`, `restaurant_review_sentiment`
- `notification`, `member_favorite_restaurant`

부가 business 데이터:

- `announcement`
- `promotion`, `promotion_asset`, `promotion_display`
- `report`
- `push_notification_target`
- `refresh_token`
- `image`, `domain_image`
- `restaurant_image`, `review_image`, `image_optimization_job`
- `restaurant_ai_results`, `ai_restaurant_feature`, `restaurant_comparison`

비동기 로그성 데이터:

- `user_activity_event`
- `user_activity_source_outbox`
- `user_activity_dispatch_outbox`
- `notification_outbox`
- `consumed_notification_event`
- `message_queue_trace_log`

bootstrap 데이터:

- `test-user-001` 형식의 부하테스트 계정군
- 고정 그룹 `2002`
- 고정 서브그룹 `4002`
- 고정 채팅방 bootstrap

## 생성 규칙

- 비동기 로그성 데이터는 고정 숫자를 박아 넣지 않고, `group.joined`, `review.created`, `ui.search.executed`, `ui.favorite.updated`, `ui.restaurant.viewed`, `ui.page.viewed`, `ui.page.dwelled` 흐름과 알림 발행 흐름을 기준으로 파생 생성합니다.
- 이미지 URL, `storage_key`, `device_id`, `fcm_token`, 딥링크 같은 캐시 민감 필드는 `run_token + 도메인 키` 기반 의사난수 해시로 생성합니다.
- 같은 `run_token`으로 생성하면 재현 가능성을 유지할 수 있습니다.
- `realistic_names=true`이면 식당명, 그룹명, 주소, 좌표를 서울권 중심의 현실적인 패턴으로 만듭니다.
- cleanup SQL은 이름뿐 아니라 `run_token`이 들어간 asset URL까지 함께 사용해 해당 배치 데이터를 식별합니다.

## 생성되지 않는 데이터

기본 생성 대상에서 제외하는 테이블:

- `ai_job`
- `batch_execution`
- `run_meta`
- `member_serach_history`

시스템/운영 관리 테이블:

- `flyway_schema_history`
- `spatial_ref_sys`

## 커스텀 프리셋

기본 기준 외에 필요한 경우만 아래를 사용합니다.

- `search_perf_test.json`: 검색 API 중심 부하를 따로 만들 때
- `config.example.json`: counts/content/tuning 조합을 직접 조정할 때
- `seed_restaurant_load.sql`: 음식점만 빠르게 추가 확보할 때

검색 성능 프리셋 예시:

```bash
cd loadtest/seed
python3 generate_dummy_seed_sql.py \
  --config search_perf_test.json \
  --output ../results/generated-seed/search-perf/seed.sql \
  --cleanup-output ../results/generated-seed/search-perf/cleanup.sql
```

기본 프리셋을 별도 산출물로 뽑고 싶을 때:

```bash
cd loadtest/seed
python3 generate_dummy_seed_sql.py \
  --config default_seed_profile.json \
  --output ../results/generated-seed/default/seed.sql \
  --cleanup-output ../results/generated-seed/default/cleanup.sql
```

## 운영 메모

- `food_category`가 비어 있으면 `restaurant_food_category`는 자동 skip됩니다.
- `keyword`가 비어 있으면 `review_keyword`는 자동 skip됩니다.
- `restaurant_ai_results`, `ai_restaurant_feature`, `restaurant_image`, `review_image`, `image_optimization_job`는 테이블 존재 여부를 확인한 뒤에만 삽입합니다.
- 대부분의 로그인 기반 suite는 `내 그룹 조회 -> 그룹 검색 -> TEST_GROUP_CODE로 가입` 흐름을 사용하므로, 시드와 테스트 환경변수는 같이 맞춰야 합니다.
