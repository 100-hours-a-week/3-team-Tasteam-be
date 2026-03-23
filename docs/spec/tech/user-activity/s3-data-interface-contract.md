# 목차

* [1. 문서 목적](#1-문서-목적)
* [2. 시스템 역할 정의](#2-시스템-역할-정의)
  * [API 서버 책임](#api-서버-책임)
  * [AI 서버 책임](#ai-서버-책임)
* [3. 전체 데이터 흐름](#3-전체-데이터-흐름)
* [4. Raw Data 계약](#4-raw-data-계약)
  * [4-1. 사용자 행동 이벤트](#4-1-사용자-행동-이벤트)
  * [4-2. 음식점 데이터](#4-2-음식점-데이터)
  * [4-3. 메뉴 집계 데이터](#4-3-메뉴-집계-데이터)
* [5. 추천 결과 계약](#5-추천-결과-계약)
* [6. 추천 결과 Import 계약](#6-추천-결과-import-계약)
* [7. 모델 버전 관리](#7-모델-버전-관리)
* [8. 추천 조회](#8-추천-조회)
* [9. 추가 논의가 필요한 사항](#9-추가-논의가-필요한-사항)

---

# 1. 문서 목적

본 문서는 **개인화 음식점 추천 시스템에서 AI 서버와 API 서버 간의 역할과 데이터 교환 방식**을 정의한다.

목표는 다음과 같다.

- 추천 시스템의 **Serving Layer와 ML Pipeline을 분리**
- 데이터 계약을 명확히 하여 **양쪽 서버의 독립적인 개발을 가능하게 함**
- 추천 결과 생성 및 서빙 과정에서 **의존성과 장애 전파를 최소화**

---

# 2. 시스템 역할 정의

## API 서버 책임

API 서버는 **Serving Layer**를 담당한다.

주요 역할

1. 사용자 행동 로그 수집
2. raw 데이터 S3 적재
3. 추천 결과 S3 → DB import
4. 추천 결과 조회 API 제공

API 서버는 다음을 수행하지 않는다.

- 모델 학습
- feature 생성
- 추천 생성
- 모델 관리

---

## AI 서버 책임

AI 서버는 **ML Pipeline**을 담당한다.

주요 역할

1. S3 raw data 기반 feature 생성
2. 학습 데이터셋 생성
3. 모델 학습
4. batch inference 수행
5. 추천 결과 파일 S3 저장
6. 모델 버전 관리

AI 서버는 다음을 수행하지 않는다.

- API 요청 처리
- 추천 결과 DB 저장
- 사용자 요청 처리

---

# 3. 전체 데이터 흐름

<img alt="mermaid-diagram" src="https://github.com/user-attachments/assets/289894f7-33f3-4cb0-9a99-ff6374356e8f" />


---

# 4. Raw Data 계약

API 서버는 다음 데이터를 **S3 Data Lake**에 저장한다.

환경별 bucket

```
tasteam-prod-analytics
tasteam-stg-analytics
tasteam-dev-analytics
```

## 4-1. 사용자 행동 이벤트

### S3 경로

```
s3://tasteam-{env}-analytics/
	raw/
		events/
			dt=YYYY-MM-DD/
				part-00001.csv
				_SUCCESS
```

### 데이터 스키마

| column | 설명 |
| --- | --- |
| event_id | 이벤트 UUID |
| event_name | 이벤트 타입 (view / click / review 등) |
| occurred_at | 이벤트 발생 시각 |
| dining_type | 식사 유형 (SOLO / GROUP) |
| distance_bucket | 사용자-음식점 거리 버킷 |
| weather_bucket | 날씨 버킷 |
| member_id | 로그인 사용자 ID |
| anonymous_id | 비로그인 사용자 ID |
| session_id | 세션 ID |
| restaurant_id | 음식점 ID |
| recommendation_id | 추천 노출 ID (nullable) |
| platform | 플랫폼 (WEB / IOS / ANDROID) |
| created_at | 이벤트 저장 시각 |

## 4-2. 음식점 데이터

### S3 경로

```
s3://tasteam-{env}-analytics/
	raw/
		restaurants/
			dt=YYYY-MM-DD/
				part-00001.csv
				_SUCCESS
```

### 데이터 스키마

| column | 설명 |
| --- | --- |
| restaurant_id | 음식점 ID |
| restaurant_name | 음식점 이름 |
| sido | 시도 |
| sigungu | 시군구 |
| eupmyeondong | 읍면동 |
| geohash | 위치 geohash |
| food_category_id | 음식 카테고리 ID |
| food_category_name | 음식 카테고리 |

## 4-3. 메뉴 집계 데이터

### S3 경로

```
s3://tasteam-{env}-analytics/
	raw/
		menus/
			dt=YYYY-MM-DD/
				part-00001.csv
				_SUCCESS
```

### 데이터 스키마

| column | 설명 |
| --- | --- |
| restaurant_id | 음식점 ID |
| menu_count | 메뉴 수 |
| price_min | 최소 가격 |
| price_max | 최대 가격 |
| price_mean | 평균 가격 |
| price_median | 중앙값 |
| representative_menu_name | 대표 메뉴 |
| top_menus | 주요 메뉴 JSON |
| price_tier | 가격대 |

---

# 5. 추천 결과 계약

AI 서버는 추천 결과를 **S3에 파일 형태로 저장한다.**

### S3 경로

```
s3://tasteam-{env}-analytics/
	recommendations/
		pipeline_version=VERSION/
			dt=YYYY-MM-DD/
				part-00001.csv
				_SUCCESS
```

### 데이터 스키마

| column | 설명 |
| --- | --- |
| user_id | 사용자 ID |
| anonymous_id | 익명 사용자 |
| restaurant_id | 음식점 ID |
| score | 추천 점수 |
| rank | 추천 순위 |
| context_snapshot | 추천 생성 시 context |
| pipeline_version | 모델 파이프라인 버전 |
| generated_at | 추천 생성 시각 |
| expires_at | 추천 만료 시각(24시간) |

---

# 6. 추천 결과 Import 계약

API 서버는 **S3 polling 기반으로 추천 결과를 DB에 적재한다.**

### Import 절차

1. S3 polling
2. 새로운 `(pipeline_version, dt)` 탐색
3. `_SUCCESS` 파일 확인
4. 추천 결과 파일 import
5. Recommendation DB 저장

### 완료 마커

AI 서버는 추천 생성 완료 후 `_SUCCESS` 파일을 생성한다.

```
recommendation_result/
	pipeline_version=deepfm-1.0.20260227/
		dt=2026-03-07/
			part-00001.csv
			_SUCCESS
```

### DB 저장 스키마

| column | 설명 |
| --- | --- |
| user_id | 사용자 ID |
| anonymous_id | 익명 사용자 |
| restaurant_id | 음식점 ID |
| score | 추천 점수 |
| rank | 추천 순위 |
| pipeline_version | 모델 버전 |
| generated_at | 추천 생성 시각 |
| expires_at | 추천 만료 시각 |

---

# 7. 모델 버전 관리

모델 버전 관리는 **AI 서버 책임**이다.

추천 결과는 항상 **pipeline_version을 포함해야 한다.**

```
deepfm-1.0.20260227
deepfm-1.1.20260301
```

---

# 8. 추천 조회

API 서버는 **현재 활성 pipeline_version의 추천 결과만 반환한다.**

```
SELECT
    rr.member_id,
    rr.anonymous_id,
    rr.restaurant_id,
    rr.score,
    rr.rank,
    rr.pipeline_version,
    rr.generated_at,
    rr.expires_at
FROM restaurant_recommendation rr
JOIN restaurant_recommendation_model rrm
  ON rrm.id = rr.model_id
WHERE rr.member_id = :memberId
  AND rrm.status = 'ACTIVE'
ORDER BY rr.rank ASC
LIMIT :limit;
```

---

# 9. 추가 논의가 필요한 사항

### 추천 생성 정책

| 항목 | 정책 |
| --- | --- |
| 추천 생성 범위 | 전체 사용자 |
| 추천 생성 방식 | batch inference |
| 추천 생성 주기 | 하루 2회 |
| 추천 TTL | 24시간 |