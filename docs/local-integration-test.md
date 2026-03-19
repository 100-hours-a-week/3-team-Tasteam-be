# 로컬 통합 테스트 실행 가이드

FE + BE + Kafka + Kafka Connect → MinIO(S3) 전체 파이프라인을 로컬에서 검증하는 절차.

---

## 사전 준비

1. `.env.local` 파일 준비 (`.env.example` 복사 후 값 입력)
2. Kafka 통합 테스트용 값으로 수정:

```env
MQ_ENABLED=true
MQ_PROVIDER=kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:29092
KAFKA_CONNECT_URL=http://localhost:8083
KAFKA_CONNECT_CONNECTOR_AUTO_REGISTER=true
KAFKA_CONNECT_HEAP_OPTS=-Xms512M -Xmx768M
KAFKA_CONNECT_MEM_LIMIT=1536m
KAFKA_CONNECT_UA_S3_TASKS_MAX=1
KAFKA_CONNECT_UA_S3_FLUSH_SIZE=100
KAFKA_CONNECT_UA_S3_ROTATE_INTERVAL_MS=60000
KAFKA_CONNECT_UA_S3_ROTATE_SCHEDULE_INTERVAL_MS=60000

ANALYTICS_S3_BUCKET=tasteam-analytics-local
S3_ENDPOINT=http://localhost:9000
KAFKA_CONNECT_S3_ENDPOINT=http://minio:9000
S3_PATH_STYLE_ACCESS=true
AWS_ACCESS_KEY_ID=minioadmin
AWS_SECRET_ACCESS_KEY=minioadmin
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin
```

---

## 실행 순서

### Step 1 — Kafka 인프라 기동

Kafka Connect 이미지 빌드 (최초 1회, S3 Sink Connector JAR 다운로드 포함):

```bash
docker compose -f docker-compose.kafka.yml build kafka-connect
```

전체 Kafka 인프라 기동 (Kafka + Kafka Connect + MinIO):

```bash
docker compose -f docker-compose.kafka.yml up -d
```

모든 서비스 healthy 확인:

```bash
docker compose -f docker-compose.kafka.yml ps
```

### Step 2 — DB/Redis 기동

```bash
docker compose -f docker-compose.local.yml up -d db redis
```

### Step 3 — BE API 로컬 실행

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew :app-api:bootRun
```

`api`는 로컬 JVM에서 실행하고, Docker는 인프라(db/redis/kafka/kafka-connect/minio)만 사용한다.
Spring 기동 시 `KAFKA_CONNECT_CONNECTOR_AUTO_REGISTER=true`이면 `user-activity-s3-sink` 커넥터가 자동 등록된다.

커넥터 등록 확인:

```bash
curl http://localhost:8083/connectors
# 출력: ["user-activity-s3-sink"]
```

주의:
- Kafka Connect는 커넥터 설정을 내부 토픽에 저장하므로, JSON 파일만 바꾸고 컨테이너만 재시작하면 이전 설정이 복원될 수 있다.
- 로컬 JSON을 수정한 뒤 기존 커넥터 값을 갱신하려면 아래 `PUT` 요청이나 `docker compose ... down -v` 재기동이 필요하다.

### Step 4 — FE 기동

```bash
# FE 레포에서
npm run dev
```

---

## 검증

### 커넥터 상태 확인

```bash
curl http://localhost:8083/connectors/user-activity-s3-sink/status | jq .
```

### MinIO 웹 콘솔 접속

`http://localhost:9001` — ID/PW: `minioadmin / minioadmin`

버킷 `tasteam-analytics-local` → `raw/events/dt=YYYY-MM-dd/` 경로에 JSON gzip 파일(`*.json.gz`) 적재 여부 확인.

### AWS CLI로 확인 (선택)

```bash
aws --endpoint-url=http://localhost:9000 \
    --region ap-northeast-2 \
    s3 ls s3://tasteam-analytics-local/raw/events/ \
    --recursive
```

> AWS CLI가 없으면 MinIO 웹 콘솔 사용.

---

## 커넥터 수동 등록 (자동 등록 미사용 시)

최초 등록:

```bash
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @config/kafka-connect/user-activity-s3-sink.local.json
```

기존 커넥터 설정 갱신:

```bash
curl -X PUT http://localhost:8083/connectors/user-activity-s3-sink/config \
  -H "Content-Type: application/json" \
  -d @config/kafka-connect/user-activity-s3-sink.local.json
```

---

## 종료

```bash
docker compose -f docker-compose.local.yml down
docker compose -f docker-compose.kafka.yml down
```

데이터 초기화(볼륨 삭제)가 필요한 경우:

```bash
docker compose -f docker-compose.kafka.yml down -v
docker compose -f docker-compose.local.yml down -v
```
