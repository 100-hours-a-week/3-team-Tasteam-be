# Dummy SQL Generator

앱의 `DummyDataSeedService` 대신 DB에 직접 실행할 `INSERT SQL`을 생성합니다.

## 1) 기본 사용

```bash
python3 scripts/dummy_sql_generator/generate_dummy_seed_sql.py
```

기본 출력 파일:
- `scripts/dummy_sql_generator/generated_dummy_seed.sql`

## 2) 커스텀 설정 사용

```bash
python3 scripts/dummy_sql_generator/generate_dummy_seed_sql.py \
  --config scripts/dummy_sql_generator/config.example.json \
  --output scripts/dummy_sql_generator/out/seed.sql \
  --cleanup-output scripts/dummy_sql_generator/out/cleanup.sql
```

## 3) 설정 미리보기

```bash
python3 scripts/dummy_sql_generator/generate_dummy_seed_sql.py --print-config
```

## 4) 실행 예시 (PostgreSQL)

```bash
psql "$DATABASE_URL" -f scripts/dummy_sql_generator/generated_dummy_seed.sql
```

## 설정 포인트

- `counts`: 테이블별 삽입량.
- `content`: 더미 문구/이름/패턴.
  - `run_token`을 비워두면 자동 생성됩니다.
  - `run_token`이 들어가므로, 세트별 식별 및 정리에 유리합니다.
  - `*_pool`, `*_tokens`, `member_email_domains` 같은 배열을 바꾸면 실제 서비스와 유사한 다양한 길이/조합의 텍스트가 생성됩니다.
- 생성 SQL는 다음을 가변적으로 채웁니다.
  - `member`: 이메일/닉네임/소개/프로필/로그인 시점
  - `group/subgroup`: 타입/가입방식/설명/이미지/도메인
  - `restaurant/menu`: 이름/주소/전화번호/메뉴 설명/이미지
  - `chat/review/notification`: 짧은 문장 + 긴 문장 혼합 포맷
- `tuning`: 메뉴 개수/가격/리뷰 키워드 수 같은 생성 규칙.

## 참고

- `food_category`가 비어있으면 `restaurant_food_category`는 자동 skip됩니다.
- `keyword`가 비어있으면 `review_keyword`는 자동 skip됩니다.
- `cleanup-output`을 지정하면 같은 `run_token` 기준 정리 SQL도 함께 생성됩니다.
