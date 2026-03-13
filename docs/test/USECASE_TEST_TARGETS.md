# 테스트 인벤토리와 보장 시나리오

이 문서는 현재 백엔드 모듈에 존재하는 `@ServiceIntegrationTest`, `@MessageQueueFlowTest`, `@PerformanceTest`
기준 테스트 인벤토리와 보장 시나리오를 정리한다.

원칙:

- 현재 코드에 있는 테스트만 기록한다.
- 아직 구현이 끝나지 않은 테스트는 완료된 계약처럼 쓰지 않는다.
- 프로덕션 유즈케이스뿐 아니라 테스트 인프라 self-check도 포함한다.

## 1. 테스트 인프라 self-check

- `JpaAuditingConflictTest`
  - 대상: 테스트 인프라/JPA Auditing 설정
  - 보장: JPA Auditing 관련 빈이 중복 없이 로드된다
  - 보장: `jpaAuditingHandler` 빈이 하나만 등록된다

## 2. Admin

- `AdminGroupImageIntegrationTest`
  - 대상: `AdminGroupService`
  - 보장: 로고 이미지와 함께 그룹을 생성하면 이미지가 `ACTIVE`로 전환된다
  - 보장: 로고 이미지 없이 생성하면 `DomainImage` 링크가 생기지 않는다
  - 보장: 그룹 목록 조회 시 로고 이미지 URL 해석이 반영된다
  - 실패: 존재하지 않는 로고 이미지를 지정하면 생성이 실패한다

- `AdminRestaurantServiceIntegrationTest`
  - 대상: `AdminRestaurantService`
  - 보장: 주소 지오코딩, 카테고리, 이미지, 주간 스케줄이 함께 저장된다
  - 보장: 상세 조회 시 카테고리와 이미지 URL이 포함된다
  - 보장: 수정 시 카테고리 변경과 이미지 재연결이 반영된다
  - 보장: 삭제 시 soft delete와 이미지 연결 제거가 함께 처리된다
  - 실패: 존재하지 않는 이미지 또는 음식점이면 실패한다

## 3. Auth / Member

- `TokenRefreshServiceIntegrationTest`
  - 대상: `TokenRefreshService`
  - 보장: 유효한 리프레시 토큰이면 새 액세스 토큰이 발급된다
  - 실패: 잘못된 형식, 만료 토큰, 비활성 회원, 저장된 토큰 없음

- `OAuthLoginServiceIntegrationTest`
  - 대상: `OAuthLoginService`
  - 보장: 신규 사용자는 회원/OAuth 계정 생성 후 로그인 처리된다
  - 보장: 기존 사용자는 기존 계정으로 로그인 처리된다
  - 보장: 탈퇴 상태 회원은 재활성화된다
  - 실패: 지원하지 않는 OAuth 공급자

- `MemberProfileImageIntegrationTest`
  - 대상: `MemberService`
  - 보장: 프로필 이미지 업데이트 후 프로필 조회와 `getMyProfile`에 이미지 URL이 반영된다
  - 실패: 존재하지 않는 파일 UUID

## 4. Group / Subgroup / Favorite

- `GroupServiceIntegrationTest`
  - 대상: `GroupFacade`
  - 보장: `EMAIL` / `PASSWORD` joinType별 생성 계약이 반영된다
  - 보장: 로고 이미지 연결 시 `PENDING -> ACTIVE` 전환이 일어난다
  - 보장: 그룹 조회/수정/회원 목록 조회가 실제 DB 상태와 일치한다
  - 보장: 이메일 인증 가입과 비밀번호 인증 가입이 성공/재가입 restore 시나리오를 처리한다
  - 보장: 그룹 탈퇴 시 soft delete와 하위그룹 cascading soft delete가 반영된다
  - 실패: 중복 이름, 잘못된 이메일 도메인, 유효하지 않은 토큰/비밀번호

- `GroupEmailRateLimitIntegrationTest`
  - 대상: `GroupFacade` + rate limit 설정
  - 보장: 이메일/IP/사용자 기준 1분 제한이 429로 동작한다
  - 보장: 일일 제한 초과 시 24시간 block 키가 생성된다
  - 보장: TTL 만료 후 재시도가 가능해진다

- `SubgroupServiceIntegrationTest`
  - 대상: `SubgroupFacade`
  - 보장: 하위그룹 생성 시 creator 자동 가입과 memberCount 반영
  - 보장: 이미지 연결 시 `PENDING -> ACTIVE` 전환
  - 보장: `OPEN` / `PASSWORD` joinType 가입, 탈퇴, restore, 수정, 상세/목록/회원 조회
  - 보장: 검색 시 정렬과 공격 문자열 차단
  - 실패: 중복 이름, 비활성 그룹, 잘못된 비밀번호, 비회원 상세 조회 제한

- `FavoriteServiceIntegrationTest`
  - 대상: `FavoriteService`
  - 보장: 내 찜은 soft delete된 데이터를 복구할 수 있다
  - 보장: 찜 타겟 조회는 내 찜과 소모임 찜 타겟을 함께 반환한다
  - 보장: 소모임 찜 등록/삭제, 삭제 멱등성, restaurantId 기준 dedup, restaurant별 집계
  - 보장: 공개 소모임은 비회원도 조회 가능하고 비공개 소모임은 차단된다
  - 실패: 내 찜 중복 등록, 생성자가 아닌 회원의 소모임 찜 삭제

- `FavoriteRestaurantImageIntegrationTest`
  - 대상: `FavoriteService`
  - 보장: 즐겨찾기 목록에서 이미지 URL 해석이 반영된다
  - 보장: 이미지 없음/빈 목록도 정상 처리된다

## 5. File / Restaurant / Review

- `FileServiceIntegrationTest`
  - 대상: `FileService`
  - 보장: presigned 업로드 생성, 도메인 이미지 연결, 대표 이미지 URL 조회, 이미지 정리 플로우
  - 보장: 이미지 교체/삭제 시 `DomainImage` 링크와 `ImageStatus`가 함께 정리된다
  - 실패: 업로드 정책 위반, 존재하지 않는 파일 UUID, 활성 상태가 아닌 이미지 URL 조회

- `GroupSubgroupImageActivationIntegrationTest`
  - 대상: `GroupService`, `SubgroupService`
  - 보장: 그룹/하위그룹 생성과 수정에서 연결된 `PENDING` 이미지가 `ACTIVE`로 전환된다
  - 실패: 존재하지 않는 이미지 지정

- `RestaurantServiceIntegrationTest`
  - 대상: `RestaurantService`
  - 보장: 음식점 생성 시 연관 엔티티 생성, 이벤트 발행, 이미지 활성화
  - 보장: 상세 조회 시 조합된 응답과 선택 데이터 null 분기
  - 보장: 수정 시 이름/이미지 변경과 롤백 계약
  - 보장: 삭제 시 soft delete, 이미지 정리, 재삭제 멱등성
  - 실패: 존재하지 않는 이미지, 존재하지 않는 음식점

- `MenuServiceIntegrationTest`
  - 대상: `MenuService`
  - 보장: 메뉴 카테고리 생성, 메뉴 생성, 메뉴 일괄 생성, 메뉴 조회
  - 실패: 존재하지 않는 음식점, 다른 음식점 카테고리, 카테고리 불일치

- `RestaurantScheduleServiceIntegrationTest`
  - 대상: `RestaurantScheduleService`
  - 보장: 주간 스케줄 생성과 조회 반영
  - 실패: 존재하지 않는 음식점

- `ReviewServiceIntegrationTest`
  - 대상: `ReviewService`
  - 보장: 리뷰 생성, 이미지 활성화, 상세 조회, soft delete
  - 실패: 존재하지 않는 키워드

- `ReviewImageDeletionIntegrationTest`
  - 대상: `ReviewService`
  - 보장: 리뷰 삭제 시 연결된 `DomainImage`가 정리된다
  - 보장: 이미지가 여러 개여도 모두 정리된다
  - 보장: 이미지가 없어도 삭제 흐름이 깨지지 않는다
  - 실패: 다른 사용자의 리뷰 삭제 시도

## 6. Main / Search / Notification / Analytics

- `MainServiceIntegrationTest`
  - 대상: `MainService`
  - 보장: 메인/홈/AI 추천 조회 시 위치 정보나 AI 요약 데이터 유무에 따른 섹션 구성이 반영된다

- `SearchServiceIntegrationTest`
  - 대상: `SearchService`
  - 보장: 그룹+음식점 통합 검색 결과와 검색 히스토리 적재
  - 실패: 잘못된 커서, 인증되지 않은 최근 검색어 조회, 존재하지 않는 최근 검색어 삭제, 공격 문자열 입력

- `NotificationServiceIntegrationTest`
  - 대상: `NotificationService`
  - 보장: 알림 생성, 페이지 조회, 정렬, 본인 데이터만 조회
  - 보장: 단일 읽음 처리, 전체 읽음 처리, 읽지 않은 개수 집계
  - 보장: 생성 → 조회 → 읽음 처리 → unread count 확인 시나리오가 연결된다
  - 실패: 존재하지 않는 알림, 다른 회원 알림 읽음 처리

- `RawDataExportServiceIntegrationTest`
  - 대상: `RawDataExportService`
  - 보장: `restaurants` / `menus` CSV가 계약 스키마 헤더와 행 값으로 적재된다
  - 보장: `_SUCCESS` 마커가 함께 업로드된다

## 7. MQ 플로우 테스트

- `NotificationMessageQueueFlowIntegrationTest`
  - 대상: `GroupMemberJoinedMessageQueuePublisher`, `NotificationMessageQueueConsumerRegistrar`
  - 보장: 그룹 가입 이벤트 발행 시 MQ publish와 notification consumer 처리까지 이어진다
  - 보장: topic, key, headers, consumer-group, payload 역직렬화 계약이 맞다
  - 실패: 잘못된 payload 수신 시 예외가 반환된다

- `UserActivityS3SinkPublisherFlowTest`
  - 대상: `UserActivityS3SinkPublisher`
  - 보장: `ActivityEvent` 발행 시 `evt.user-activity.s3-ingest.v1` 토픽에 실제 Kafka 메시지가 적재된다
  - 보장: `QueueMessageEnvelope` 역직렬화 후 `UserActivityS3Event` 14개 컬럼이 기대값과 일치한다
  - 보장: message key가 `memberId` 기준으로 직렬화되고 `occurredAt`/headers/messageId 계약이 유지된다

## 8. 성능 / 동시성 테스트

- `TokenRefreshServiceConcurrencyPerformanceTest`
  - 대상: `TokenRefreshService`
  - 보장: 동일 토큰 동시 재발급과 서로 다른 토큰 동시 재발급이 모두 성공한다
  - 보장: 최종 refresh token 저장 상태가 일관된다

- `FavoriteConcurrencyPerformanceTest`
  - 대상: `FavoriteService`
  - 보장: 같은 내 찜을 동시에 등록해도 최종 활성 상태는 1건으로 수렴한다

- `SearchServiceConcurrencyIntegrationTest`
  - 대상: `SearchService`
  - 보장: 100명 동시 검색, 동일 사용자 동시 검색, 동일 키워드 동시 검색에서 검색 히스토리가 기대대로 기록된다
  - 참고: 클래스명과 `@DisplayName`은 통합 테스트처럼 보이지만 실제 실행 태그는 `perf`다
