# Kafka / Kafka Connect 운영 분리안

## 전제

- 이 문서는 Kafka 전용 EC2와 Kafka Connect 전용 EC2를 분리 운영하는 기준이다.
- 백엔드 API 서버는 별도 EC2에서 실행된다.
- 현재 레포의 [docker-compose.kafka.yml](/Users/gy/Study/tasteam-be/docker-compose.kafka.yml)는 로컬 통합 테스트용으로 유지한다.

## 파일 분리 전략

- 로컬: `docker-compose.kafka.yml`
- Kafka 운영: `docker-compose.kafka.prod.yml`
- Kafka Connect 운영: `docker-compose.kafka-connect.prod.yml`
- 로컬 커넥터: `config/kafka-connect/user-activity-s3-sink.local.json`
- 운영 커넥터: `config/kafka-connect/user-activity-s3-sink.prod.json`
- Kafka 환경 템플릿: `.env.kafka.prod.template`
- Kafka Connect 환경 템플릿: `.env.kafka-connect.prod.template`

운영 파일도 레포에서 함께 관리하되, 실제 secret 값은 서버의 `.env.kafka.prod`, `.env.kafka-connect.prod`에만 둔다.

## 운영 파일 구성 원칙

### 1. 로컬 전용 요소 제거

- `localhost` advertised listener 제거
- `MinIO`, `minio-init` 제거
- `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD` 기반 credential 제거

### 2. 인스턴스 간 직접 접근 가능해야 함

- Kafka broker 외부 진입 주소는 Kafka EC2의 private IP 또는 내부 DNS 사용
- Kafka Connect는 Kafka EC2의 private IP 또는 내부 DNS를 통해 broker에 접근
- Kafka Connect REST는 백엔드 EC2에서 접근 가능한 private IP 또는 내부 DNS 사용

### 3. S3는 실제 AWS S3 기준으로 설정

- `s3.bucket.name`, `aws.access.key.id`, `aws.secret.access.key`, `s3.region` 사용
- MinIO용 `store.url`, `s3.endpoint`, `s3.path.style.access`는 운영에서는 제거

## 적용 파일

### `docker-compose.kafka.prod.yml`

- Kafka broker만 포함
- Kafka advertised listener를 `${KAFKA_EXTERNAL_HOST}:29092`로 노출

### `docker-compose.kafka-connect.prod.yml`

- Kafka Connect만 포함
- `CONNECT_BOOTSTRAP_SERVERS`로 Kafka EC2에 직접 접속
- Kafka Connect REST를 `${KAFKA_CONNECT_EXTERNAL_HOST}:8083` 기준으로 노출

### `config/kafka-connect/user-activity-s3-sink.prod.json`

- Kafka Connect S3 Sink 등록용 운영 템플릿
- `payload` field extraction 유지
- 운영 S3 버킷과 AWS credential 기반 설정 사용

### `.env.kafka.prod.template`

- Kafka EC2에서 실제 `.env.kafka.prod` 생성 시 사용할 템플릿
- `KAFKA_EXTERNAL_HOST`, `KAFKA_CONTROLLER_HOST`, `KAFKA_CLUSTER_ID`를 반드시 채워야 한다

### `.env.kafka-connect.prod.template`

- Kafka Connect EC2에서 실제 `.env.kafka-connect.prod` 생성 시 사용할 템플릿
- `KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONNECT_EXTERNAL_HOST`, `AWS_*`, `ANALYTICS_S3_BUCKET`을 반드시 채워야 한다

## EC2 실행 절차

### 1. Kafka EC2 파일 준비

```bash
cp .env.kafka.prod.template .env.kafka.prod
vi .env.kafka.prod
sudo apt-get update -y
sudo apt-get install -y jq gettext-base
```

### 2. Kafka EC2 기동

```bash
docker compose --env-file .env.kafka.prod -f docker-compose.kafka.prod.yml up -d
docker compose --env-file .env.kafka.prod -f docker-compose.kafka.prod.yml ps
```

### 3. Kafka Connect EC2 파일 준비

```bash
cp .env.kafka-connect.prod.template .env.kafka-connect.prod
vi .env.kafka-connect.prod
sudo apt-get update -y
sudo apt-get install -y jq gettext-base
```

### 4. Kafka Connect EC2 기동

```bash
docker compose --env-file .env.kafka-connect.prod -f docker-compose.kafka-connect.prod.yml build kafka-connect
docker compose --env-file .env.kafka-connect.prod -f docker-compose.kafka-connect.prod.yml up -d
docker compose --env-file .env.kafka-connect.prod -f docker-compose.kafka-connect.prod.yml ps
```

### 5. Kafka Connect 상태 확인

```bash
curl http://<kafka-connect-ec2-private-ip>:8083/connectors
```

### 6. 커넥터 등록

운영 커넥터 JSON은 placeholder 치환이 필요하다. 단순 `curl -d @...json`으로는 `${...}`가 치환되지 않는다.

예시:

```bash
set -a
source .env.kafka-connect.prod
set +a
envsubst < config/kafka-connect/user-activity-s3-sink.prod.json \
  | curl -X POST http://<kafka-connect-ec2-private-ip>:8083/connectors \
      -H "Content-Type: application/json" \
      -d @-
```

이미 등록된 경우:

```bash
set -a
source .env.kafka-connect.prod
set +a
envsubst < config/kafka-connect/user-activity-s3-sink.prod.json \
  | jq '.config' \
  | curl -X PUT http://<kafka-connect-ec2-private-ip>:8083/connectors/user-activity-s3-sink/config \
      -H "Content-Type: application/json" \
      -d @-
```

## 백엔드 서버 설정

백엔드가 별도 EC2에서 실행되므로 최소 아래 값이 필요하다.

```env
MQ_ENABLED=true
MQ_PROVIDER=kafka
KAFKA_BOOTSTRAP_SERVERS=<kafka-ec2-private-ip>:29092
KAFKA_CONNECT_URL=http://<kafka-connect-ec2-private-ip>:8083
KAFKA_CONNECT_CONNECTOR_AUTO_REGISTER=false
ANALYTICS_S3_BUCKET=<same-s3-bucket>
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=<aws-access-key>
AWS_SECRET_ACCESS_KEY=<aws-secret-key>
```

운영에서는 백엔드에서 Kafka Connect를 자동 등록하기보다, Kafka Connect 전용 EC2에서 수동 등록하는 쪽이 안정적이다.

## 현재 프로젝트와의 불일치 및 연동 리스크

### 1. 로컬 compose의 advertised listener 불일치

[docker-compose.kafka.yml](/Users/gy/Study/tasteam-be/docker-compose.kafka.yml#L15)는 `PLAINTEXT_HOST://localhost:29092`를 사용한다.

- 로컬에서는 맞다.
- 별도 백엔드 EC2가 붙는 운영에서는 잘못된 메타데이터가 전달되어 연결이 깨진다.

### 2. 로컬 compose는 MinIO 전제

[docker-compose.kafka.yml](/Users/gy/Study/tasteam-be/docker-compose.kafka.yml#L72)~[docker-compose.kafka.yml](/Users/gy/Study/tasteam-be/docker-compose.kafka.yml#L119)는 MinIO credential, MinIO service, bucket init을 포함한다.

- 운영에서 실제 S3를 쓸 경우 그대로 사용할 수 없다.
- 운영은 AWS credential과 실제 S3 bucket 기준으로 분리해야 한다.

### 3. connector 등록 방식 차이

[KafkaConnectConnectorRegistrar.java](/Users/gy/Study/tasteam-be/app-api/src/main/java/com/tasteam/infra/messagequeue/KafkaConnectConnectorRegistrar.java#L34)는 백엔드 애플리케이션이 Kafka Connect에 REST 호출해 커넥터를 등록하는 구조다.

- 백엔드와 Kafka Connect가 같은 로컬 머신일 때는 편하다.
- 운영에서 인스턴스가 분리되면 네트워크/보안그룹/기동 순서에 따라 실패 가능성이 커진다.

### 4. 메시지 포맷 계약 의존성

[JsonQueueMessageSerializer.java](/Users/gy/Study/tasteam-be/app-api/src/main/java/com/tasteam/infra/messagequeue/serialization/JsonQueueMessageSerializer.java#L31)는 Kafka에 envelope 형태로 넣고, 커넥터는 `payload`만 추출한다.

- 운영 커넥터에서 `ExtractField$Value` transform이 빠지면 S3 적재 포맷이 달라진다.
- downstream 분석 배치가 payload 단일 레코드를 기대하면 바로 깨질 수 있다.

### 5. 토픽명 고정 의존성

[QueueTopic.java](/Users/gy/Study/tasteam-be/app-api/src/main/java/com/tasteam/infra/messagequeue/QueueTopic.java#L10) 기준 사용자 활동 적재 토픽은 `evt.user-activity.s3-ingest.v1`이다.

- 운영 커넥터 JSON의 `topics`
- 백엔드의 `USER_ACTIVITY_S3_INGEST_MQ_TOPIC`
- DLQ topic

이 세 값이 서로 맞아야 한다.

### 6. 단일 브로커 한계

현재 Kafka 운영 compose는 단일 broker/KRaft 노드 기준이다.

- `replication.factor=1`
- Kafka Connect internal topic replication factor도 1

즉, 운영 분리는 되지만 HA 구성은 아니다. 인스턴스 장애 시 복구 시간이 필요하다.

## 권장 운영 방식

1. Kafka 전용 EC2에서 `docker-compose.kafka.prod.yml`로 broker만 운영한다.
2. Kafka Connect 전용 EC2에서 `docker-compose.kafka-connect.prod.yml`로 Connect만 운영한다.
3. Kafka Connect 커넥터는 Kafka Connect 전용 EC2에서 수동 등록한다.
4. 백엔드 EC2는 `KAFKA_BOOTSTRAP_SERVERS=<kafka-ec2-private-ip>:29092`와 `KAFKA_CONNECT_URL=http://<kafka-connect-ec2-private-ip>:8083`로 접근한다.
5. 실제 secret 값은 각 EC2의 `.env` 파일에만 두고 레포에는 커밋하지 않는다.
