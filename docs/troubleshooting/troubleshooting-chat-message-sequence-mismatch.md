# Chat - chat_message PK 중복 시퀀스 불일치 문제

## 증상

스테이징 환경에서 채팅 메시지 전송 시 아래 예외가 반복 발생했다.

```text
ERROR: duplicate key value violates unique constraint "chat_message_pkey"
Detail: Key (id)=(7812) already exists.
```

- 발생 지점: `ChatController.sendMessage(..)` -> `ChatService.sendMessage(..)`
- 영향: 메시지 저장 실패, 채팅 전송 API 500 응답

## 원인

문제의 본질은 `chat_message.id` 컬럼 기본 시퀀스와 JPA 엔티티가 참조하는 시퀀스가 서로 달라진 상태였다.

### 1. 스키마 생성 순서에서 시퀀스가 이중화됨

- [V1__init.sql](/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/app-api/src/main/resources/db/migration/V1__init.sql) 에서 `chat_message_id_seq`, `chat_room_id_seq` 같은 chat 계열 시퀀스를 먼저 생성했다.
- 이후 [V202602111000__create_chat_tables.sql](/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/app-api/src/main/resources/db/migration/V202602111000__create_chat_tables.sql) 에서 `BIGSERIAL`로 chat 테이블을 만들었다.
- PostgreSQL은 이미 같은 이름의 시퀀스가 존재하므로, 컬럼 기본 시퀀스로 `chat_message_id_seq1` 같은 새 시퀀스를 추가 생성했다.

### 2. 애플리케이션은 기존 시퀀스를 계속 사용함

- [ChatMessage.java](/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/app-api/src/main/java/com/tasteam/domain/chat/entity/ChatMessage.java) 는 `chat_message_id_seq`를 `@SequenceGenerator`로 사용한다.
- 결과적으로:
  - 테이블 기본값: `chat_message_id_seq1`
  - JPA insert 시 사용 시퀀스: `chat_message_id_seq`

### 3. 스테이징 DB에서 직접 확인한 값

```sql
SELECT COUNT(*), MAX(id) FROM chat_message;
-- 33233, 33233

SELECT last_value, is_called FROM chat_message_id_seq;
-- 7812, true

SELECT pg_get_serial_sequence('chat_message', 'id');
-- public.chat_message_id_seq1

SELECT column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'chat_message'
  AND column_name = 'id';
-- nextval('chat_message_id_seq1'::regclass)
```

즉, JPA는 `7804 ~ 7812` 같은 이미 존재하는 PK를 계속 시도했고, 그 결과 `chat_message_pkey` 충돌이 발생했다.

## 왜 `setval`만으로는 부족했는가

`chat_message_id_seq`를 `MAX(id)`로 한 번만 보정하면 당장 `chat_message` insert는 통과할 수 있다.

하지만 이 경우에도 문제는 남는다.

- 테이블 기본값은 여전히 `chat_message_id_seq1`
- 애플리케이션은 계속 `chat_message_id_seq`
- SQL 직접 insert, 배치, 관리성 쿼리, 후속 마이그레이션이 두 시퀀스를 번갈아 사용할 수 있음

즉, `setval`만으로는 증상 완화만 되고, 시퀀스 분기 자체는 제거되지 않는다.

## 해결

### 1. chat 테이블 4종의 기본 시퀀스를 하나로 통일

다음 테이블을 모두 정리했다.

- `chat_room`
- `chat_room_member`
- `chat_message`
- `chat_message_file`

추가한 마이그레이션: [V202603141730__align_chat_sequence_defaults.sql](/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/app-api/src/main/resources/db/migration/V202603141730__align_chat_sequence_defaults.sql)

이 마이그레이션은 각 테이블에 대해:

1. `MAX(id)`를 조회
2. 애플리케이션이 참조하는 `*_id_seq`를 `MAX(id)`로 맞춤
3. 컬럼 기본값을 `nextval('<expected_seq>'::regclass)`로 변경
4. `OWNED BY`를 다시 연결

### 2. 회귀 테스트 추가

[ChatSequenceAlignmentRepositoryTest.java](/Users/devon.woo/Workspace/Tasteam/3-team-Tasteam-be/app-api/src/test/java/com/tasteam/domain/chat/repository/ChatSequenceAlignmentRepositoryTest.java) 에서 다음을 검증한다.

- 충돌 상태(`*_seq1` 기본값 + JPA는 `*_seq`)를 재현
- 마이그레이션 SQL 실행
- JPA 저장과 SQL 기본 insert가 같은 시퀀스를 타는지 확인

실행 명령:

```bash
./gradlew :app-api:test --tests com.tasteam.domain.chat.repository.ChatSequenceAlignmentRepositoryTest
```

## 재발 방지 포인트

- 시퀀스를 명시적으로 선생성한 테이블에는 `BIGSERIAL` 대신 `BIGINT DEFAULT nextval('...')` 또는 명시적 시퀀스 전략을 일관되게 사용한다.
- 스키마 생성 SQL과 엔티티 `@SequenceGenerator` 이름이 실제 DB 기본값과 일치하는지 반드시 확인한다.
- 운영 DB 확인 시에는 `setval`보다 먼저 `pg_get_serial_sequence`와 `column_default`를 같이 본다.
- 유사 패턴이 있는 테이블은 한 번에 정리한다. 이번 케이스도 `chat_message`만 수정하지 않고 chat 4개 테이블을 함께 맞췄다.
