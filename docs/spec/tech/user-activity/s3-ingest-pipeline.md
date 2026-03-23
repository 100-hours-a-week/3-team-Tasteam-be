# 1. 문서 목적

본 문서는 **사용자 행동 이벤트를 Kafka 기반 스트림으로 수집하고 S3 Data Lake에 적재하는 파이프라인 설계**를 정의한다.

목표

- 사용자 이벤트를 **신뢰성 있게 수집**
- Kafka 기반 **비동기 이벤트 스트림 구축**
- S3 기반 **분석 및 ML 파이프라인용 Data Lake 구축**
- 이벤트 저장 구조를 **표준화하여 downstream 시스템과 데이터 계약을 명확히 함**

---

# 2. 이벤트 수집 파이프라인

사용자 행동 이벤트는 다음 흐름으로 저장된다.

```
Client
   ↓
API Server
   ↓
Kafka Topic
   ↓
Kafka S3 Sink
   ↓
S3 Data Lake
```

설계 목표

- 이벤트 수집을 **비동기 스트림 구조로 처리**
- Kafka를 **내결함 이벤트 버퍼**로 활용
- S3에 **분석 및 ML 파이프라인에서 사용할 Raw Data 저장**

---

# 3. Kafka Event Stream 설계

### Topic

```
user-activity-events
```

### Partition Key

```
null
```

목적

- 동일 사용자 이벤트 순서 유지 불필요
- 라운드 로빈 방식으로 부하 분산

---

### Kafka 설정

| 항목 | 값 |
| --- | --- |
| partition | 8 |
| replication factor | 3 |
| retention | 7일 |

Kafka는 **이벤트 버퍼 및 재처리 가능한 로그 저장소 역할**을 수행한다.

---

# 4. Kafka → S3 Sink

Kafka 이벤트는 Consumer를 통해 S3 Data Lake에 저장된다.

**Kafka Connect S3 Sink** 사용하여 구현한다.

이유

- 안정적인 offset 관리
- 자동 재처리
- 운영 편의성

---

# 5. S3 Data Lake 구조

환경별 bucket

```
tasteam-prod-analytics
tasteam-stg-analytics
tasteam-dev-analytics
```

사용자 행동 이벤트 저장 경로

```
s3://tasteam-{env}-analytics/
    raw/
        events/
            dt=YYYY-MM-DD/
                part-00001.csv
                part-00002.csv
                _SUCCESS
```

구성

| 파일 | 설명 |
| --- | --- |
| part-xxxxx.csv | 이벤트 데이터 |
| _SUCCESS | 데이터 생성 완료 마커 |

---

# 6. 사용자 이벤트 데이터 스키마

Kafka 이벤트는 다음 스키마로 S3에 저장된다.

| column | 설명 |
| --- | --- |
| event_id | 이벤트 UUID |
| event_name | 이벤트 타입 |
| event_version | 이벤트 스키마 버전 |
| occurred_at | 이벤트 발생 시각 |
| dining_type | 식사 유형 |
| distance_bucket | 사용자-음식점 거리 버킷 |
| weather_bucket | 날씨 버킷 |
| member_id | 로그인 사용자 ID |
| anonymous_id | 비로그인 사용자 ID |
| session_id | 세션 ID |
| restaurant_id | 음식점 ID |
| recommendation_id | 추천 노출 ID |
| platform | 플랫폼 |
| created_at | 이벤트 저장 시각 |

---

# 7. 파일 생성 정책

Kafka Sink는 이벤트를 일정 단위로 묶어 파일로 저장한다.

| 항목 | 값 |
| --- | --- |
| flush interval | 60분 |
| max events | 100,000 |
| max file size | 100MB |

목적

- S3 small file 문제 방지
- 분석 시스템 처리 효율 개선

---

# 8. 데이터 파티셔닝 정책

데이터는 **event_time 기준으로 partition 된다.**

```
dt=YYYY-MM-DD
```

예시

```
s3://tasteam-prod-analytics/raw/events/dt=2026-03-11/
```

장점

- Athena / Spark / Presto 분석 효율
- 데이터 관리 용이

---

# 9. 완료 마커

데이터 생성 완료를 나타내기 위해 `_SUCCESS` 파일을 사용한다.

```
part-00001.csv
part-00002.csv
_SUCCESS
```

downstream 시스템은 `_SUCCESS` 존재 시 데이터를 처리한다.

---

# 10. 장애 대응 전략

## Kafka 장애

- Kafka는 7일간 이벤트를 저장한다.
- consumer 장애 발생 시 재처리 가능하다.

## S3 Sink 장애

- Sink 장애 발생 시 offset commit이 중단된다.
- consumer 재시작 시 자동 재처리된다.

## 이벤트 중복

- Kafka는 **at-least-once delivery** 특성을 가진다.
- 이벤트는 `event_id` 필드를 포함하여 downstream 시스템에서 dedup 가능하다.

---

# 11. 운영 모니터링

## Kafka

- consumer lag
- produce rate
- error rate

## S3 Sink

- upload latency
- upload failure
- file creation rate

---

# 12. Late Event 처리 정책

## 파티션 기준

이벤트 데이터의 S3 파티션은 **Kafka 수신 시각이 아니라** `occurred_at` **기준으로 결정한다.**

즉 이벤트가 늦게 도착하더라도 `occurred_at`이 속한 날짜의 파티션에 저장한다.

사용자 행동 분석 및 ML 학습 데이터 생성 시 데이터 의미를 유지하기 위해 사용한다. 

## Grace Period 정책

이벤트는 네트워크 지연, 애플리케이션 재시도, Kafka 처리 지연 등으로 인해 늦게 도착할 수 있다.

이를 고려하여 각 날짜 파티션은 자정 즉시 종료하지 않고 **익일 일정 시간 동안 쓰기 가능 상태로 유지한다.**

| 항목 | 값 |
| --- | --- |
| 파티션 기준 | occurred_at |
| grace period | 익일 00:30 |
| 파일 업로드 단위 | 5분 또는 100MB |

## _SUCCESS 파일 생성 정책

`_SUCCESS` 파일은 해당 dt **파티션 내의 파일 업로드 종료를 의미한다.**

각 날짜 파티션은 grace period 종료 후 `_SUCCESS` 파일을 생성한다.

`_SUCCESS` 파일 생성 이후에는 해당 파티션 내에 새로운 파일을 추가하지 않는다.

---

# 13. 데이터 재처리 전략

## **Offset Commit 정책**

Kafka → S3 파이프라인에서는 데이터 유실을 방지하기 위해 S3 업로드 완료 후 offset commit 원칙을 따른다.

즉 이벤트는 Kafka에서 읽은 후 S3에 안전하게 저장된 뒤에만 offset을 commit한다.

이 방식은 장애 발생 시 동일 이벤트가 재처리될 수 있는 **at-least-once delivery** 특성을 가지며, 
데이터 유실보다 중복 가능성을 허용하는 방식이다.