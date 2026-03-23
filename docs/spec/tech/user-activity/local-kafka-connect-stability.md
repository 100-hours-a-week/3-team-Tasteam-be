| 항목 | 내용 |
|---|---|
| 문서 제목 | 로컬 Kafka Connect 안정화 변경 정리 |
| 문서 목적 | 로컬 Kafka Connect OOM 대응 과정에서 무엇이 왜 바뀌었는지, 운영 시 어떤 점을 주의해야 하는지 빠르게 전달한다. |
| 작성 및 관리 | Backend Team |
| 최종 수정일 | 2026.03.19 |
| 문서 버전 | v1.0 |

<br>

# 로컬 Kafka Connect 안정화 변경 정리

---

# **[1] 문제 배경**

- 로컬 통합 테스트 중 `tasteam-kafka-connect` 컨테이너가 `exit 137`로 종료되며 Kafka Connect가 내려갔다.
- 종료 시점의 커넥터 설정은 `tasks.max=8`, `flush.size=1`, `rotate.interval.ms=10000`, `rotate.schedule.interval.ms=10000`이었다.
- `docker-compose.kafka.yml`에는 `restart` 정책과 메모리 가드레일이 없어 한 번 OOM이 나면 컨테이너가 그대로 중단됐다.
- 커넥터 설정 일부가 `KafkaConnectConnectorRegistrar`에 하드코딩되어 있어 환경별 튜닝 값을 코드 수정 없이 바꾸기 어려웠다.

---

# **[2] 변경 전 / 후 비교**

| 구분 | 변경 전 | 변경 후 | 이유 |
|---|---|---|---|
| Kafka Connect 재시작 정책 | 없음 | `restart: unless-stopped` | OOM 후 자동 복구 |
| Kafka Connect 메모리 가드레일 | 없음 | `mem_limit=1536m`, `KAFKA_HEAP_OPTS=-Xms512M -Xmx768M` | 무제한 메모리 사용 방지 |
| 로컬 `tasks.max` | `8` | `1` | 대상 토픽이 단일 파티션이라 병렬 task 증가 이점이 없음 |
| 로컬 `flush.size` | `1` | `100` | 레코드 1건마다 파일 commit 되던 과도한 부하 완화 |
| 로컬 rotate 주기 | `10초` | `60초` | 초소형 gzip 파일 과다 생성 억제 |
| 커넥터 튜닝 주입 방식 | registrar 내부 하드코딩 | `application.yml` + `application.local.yml` + env 기반 | 환경별 제어 가능 |
| 예제 환경 변수 | Connect 튜닝 항목 부재 | heap/mem/tuning env 추가 | 로컬 실행 기준을 명시 |

---

# **[3] 왜 이전 값이 있었는가**

- Git 이력상 2026-03-14에 로컬 커넥터 JSON과 자동 등록 코드에 같은 값이 함께 들어갔다.
- 커밋 메시지나 문서에 명시적인 사유는 남아 있지 않았다.
- 값의 성격상 `MinIO에 파일이 바로 떨어지는지`를 로컬에서 빠르게 확인하려는 목적이었던 것으로 추정된다.
- 다만 이 값은 "검증 속도"에는 유리해도 "장시간 안정성"에는 불리했다.
- 특히 `evt.user-activity.s3-ingest.v1` 토픽이 단일 파티션인 상태에서는 `tasks.max=8`이 사실상 의미가 없었다.

---

# **[4] 이번에 바뀐 설정 경로**

## **[4-1] 인프라**

- `docker-compose.kafka.yml`
  - `restart: unless-stopped`
  - `mem_limit: ${KAFKA_CONNECT_MEM_LIMIT:-1536m}`
  - `KAFKA_HEAP_OPTS: "${KAFKA_CONNECT_HEAP_OPTS:--Xms512M -Xmx768M}"`

## **[4-2] 커넥터 기본값**

- `config/kafka-connect/user-activity-s3-sink.local.json`
  - `tasks.max=1`
  - `flush.size=100`
  - `rotate.interval.ms=60000`
  - `rotate.schedule.interval.ms=60000`

## **[4-3] 애플리케이션 설정**

- `app-api/src/main/resources/application.yml`
  - 공통 기본값을 env 기반으로 노출
- `app-api/src/main/resources/application.local.yml`
  - 로컬 기본값을 안정화 기준으로 override
- `app-api/src/main/java/com/tasteam/infra/messagequeue/KafkaMessageQueueProperties.java`
  - `tasksMax`, `flushSize`, `rotateIntervalMs`, `rotateScheduleIntervalMs` 추가
- `app-api/src/main/java/com/tasteam/infra/messagequeue/KafkaConnectConnectorRegistrar.java`
  - 하드코딩 제거, properties 기반 config 생성

## **[4-4] 테스트**

- `KafkaConnectConnectorRegistrarTest`
  - 자동 등록 시 커넥터 설정이 properties 값을 반영하는지 검증
- `KafkaMessageQueuePropertiesBindingTest`
  - 로컬 S3 Sink 튜닝 값이 바인딩되는지 검증

---

# **[5] 운영 시 주의할 점**

## **[5-1] Kafka Connect는 재시작만으로 JSON을 다시 읽지 않는다**

- Kafka Connect는 커넥터 설정을 내부 토픽(`tasteam.connect.*`)에 저장한다.
- 따라서 `config/kafka-connect/user-activity-s3-sink.local.json`만 수정하고 컨테이너를 재시작해도, 이미 등록된 커넥터는 이전 설정으로 복원될 수 있다.
- 설정 변경을 실제 런타임에 반영하려면 아래 둘 중 하나가 필요하다.
  - `PUT /connectors/user-activity-s3-sink/config`로 갱신
  - `docker compose -f docker-compose.kafka.yml down -v` 후 재기동

예시:

```bash
curl -X PUT http://localhost:8083/connectors/user-activity-s3-sink/config \
  -H "Content-Type: application/json" \
  -d @config/kafka-connect/user-activity-s3-sink.local.json
```

## **[5-2] 로컬에서 가장 먼저 확인할 신호**

```bash
docker compose -f docker-compose.kafka.yml ps kafka-connect
curl http://localhost:8083/connectors/user-activity-s3-sink/status | jq .
curl http://localhost:8083/connectors/user-activity-s3-sink/config | jq .
```

확인 포인트:
- 컨테이너 `STATUS`가 `healthy`인지
- 커넥터와 task가 `RUNNING`인지
- `tasks.max`, `flush.size`, `rotate.interval.ms`가 기대값인지

---

# **[6] 리뷰어가 보면 좋은 차이점**

- 이번 변경은 "Kafka Connect를 새로 도입"한 것이 아니라 "이미 붙어 있던 로컬 Kafka Connect를 오래 버티게 정리"한 작업이다.
- 핵심은 세 가지다.
  - Connect 컨테이너에 복구/메모리 가드레일 추가
  - 로컬 S3 Sink 값을 공격적인 즉시 flush 모드에서 안정화 모드로 완화
  - 하드코딩을 설정 기반으로 바꿔 환경별 조정 가능하게 변경
- 따라서 리뷰 시에는 기능 추가보다 `로컬 운영 안정성`, `설정 주입 경로`, `기존 자동 등록 동작 유지 여부`를 중심으로 보면 된다.

---

# **[7] 관련 문서**

- 로컬 실행 절차: `docs/local-integration-test.md`
- 사용자 이벤트 수집 전체 구조: `docs/spec/tech/user-activity/README.md`
- 장애 대응 절차: `docs/spec/tech/user-activity/RUNBOOK.md`
