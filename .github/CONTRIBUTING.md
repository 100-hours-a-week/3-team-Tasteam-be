# 컨트리뷰팅 가이드

## 1. 전체 워크플로우

1. Issue/작업 범위 정의
2. 브랜치 생성 (`feature/*` 등)
3. 작업 + 로컬 테스트 실행
4. 커밋 (Husky 훅을 통과해야 함)
5. GitHub에 브랜치 푸시
6. PR 생성 → 리뷰 → 머지

---

## 2. 브랜치 전략

- 메인 브랜치
  - `main`: 배포용 안정 브랜치
  - `develop`: 개발용 통합 브랜치
- 작업 브랜치 (예시)
  - `feature/<scope>`: 기능 추가 (`feature/post-like`, `feature/webflux-ai` 등)
  - `fix/<scope>`: 버그 수정 (`fix/login-error`, `fix/file-upload-timeout` 등)
  - `refactor/<scope>`: 리팩터링 (`refactor/querydsl-post`, …)
  - `docs/<scope>`: 문서 작업
- 규칙
  - 브랜치 이름에는 **작업 범위가 드러나도록** 작성합니다.
  - 단일 PR에는 가급적 **하나의 논리적 변경**만 포함합니다.

---

## 3. 커밋 컨벤션 (Husky 연동)

- 커밋 메시지 첫 줄 포맷
  - `타입(소문자): 설명` 형식으로 작성하면, Husky가 자동으로 **이모지 + 타입**으로 변환합니다.
  - 예:
    - `feat: 게시글 좋아요 기능 추가`
    - `fix: 로그인 실패 예외 처리 수정`
    - `docs: WebFlux README 추가`
- 허용 타입
  - `feat`, `fix`, `perf`, `refactor`, `test`, `docs`, `style`, `chore`, `delopy`, `revert`
  - 다른 타입으로 시작하면 커밋이 거절됩니다.
- Git 기본 메시지
  - `Merge`, `Revert` 등 Git이 자동 생성한 메시지는 예외 처리됩니다.

자세한 규칙과 훅 동작은 Husky 문서를 참고하세요.
- [HUSKY_COMMIT_MESSAGE_CONVENTION.md](../docs/husky/HUSKY_COMMIT_MESSAGE_CONVENTION.md)
- [HUSKY_SETUP_SUMMARY.md](../docs/husky/HUSKY_SETUP_SUMMARY.md)

---

## 4. PR 규칙

- PR 제목
  - 가능하면 커밋 타입과 유사한 형태로 간단히 요약합니다.
    - 예: `[feat] 게시글 조회 API 정렬 옵션 추가`
- PR 설명
  - **무엇을 / 왜** 수정했는지 3~5줄 이내로 요약
  - 주요 변경 파일/영향 범위, 테스트 여부를 간단히 명시
- 리뷰
  - 큰 PR보다는 **자주, 작은 단위**의 PR을 선호합니다.
  - 리뷰 코멘트에 맞춰 추가 커밋을 푸시합니다. (가능하면 force-push 지양)

---

## 5. 테스트 & 테스트 컨벤션

- 로컬에서 최소한 아래를 확인해 주세요.
  - 단위/통합 테스트: `./gradlew test`
  - (선택) 필요 시 모듈별 테스트 실행
- 테스트 작성 원칙 (요약)
  - **도메인/정책 로직**: 유닛 테스트 위주
  - **JPA/쿼리**: 리포지토리 통합 테스트
  - **서비스/보안/필터**: 통합 테스트 또는 슬라이스 테스트
  - **컨트롤러**: `@WebMvcTest` 또는 통합 테스트 기반 API 검증

자세한 가이드는 테스트 문서를 참고하세요.
- [PROJECT_TEST_STRATEGY.md](../docs/test/PROJECT_TEST_STRATEGY.md)
- [DOMAIN_TEST_CONVENTION.md](../docs/test/DOMAIN_TEST_CONVENTION.md)
- [SERVICE_TEST_CONVENTION.md](../docs/test/SERVICE_TEST_CONVENTION.md)
- [REPOSITORY_TEST_CONVENTION.md](../docs/test/REPOSITORY_TEST_CONVENTION.md)
- [CONTROLLER_WEBMVC_TEST_CONVENTION.md](../docs/test/CONTROLLER_WEBMVC_TEST_CONVENTION.md)
- [INTEGRATION_TEST_CONVENTION.md](../docs/test/INTEGRATION_TEST_CONVENTION.md)

---

## 6. CI / CD 파이프라인

- GitHub Actions CI
  - 워크플로우: [`workflows/ci.yml`](workflows/ci.yml)
  - 동작:
    - `main`, `develop` 브랜치로의 push 시 실행
    - `docs/**`, `*.md` 만 변경된 커밋은 자동으로 CI 스킵
    - `./gradlew test` 실행 후, `./gradlew build -x test` 빌드 수행
    - 테스트 리포트 및 JaCoCo 커버리지를 아티팩트로 업로드
- Husky `pre-push` 훅
  - 로컬에서 `git push` 시
    - `./gradlew test jacocoTestCoverageVerification` 실행
    - 실패하면 푸시가 차단됨

CD(배포)는 현재 별도 스크립트/인프라 레포 또는 수동 절차를 사용하며,  
이 레포에서는 **빌드·테스트·커버리지 기준을 보장하는 것**에 집중합니다.

---

## 7. 기여 체크리스트

PR을 열기 전에 아래 항목을 한 번 더 확인해 주세요.

- [ ] 브랜치 이름이 작업 범위를 잘 표현하는가?
- [ ] 커밋 메시지가 컨벤션(Husky)을 통과하는가?
- [ ] 관련 테스트를 작성/수정했고, 로컬에서 성공하는가?
- [ ] CI (GitHub Actions) 상태가 초록(성공)인가?
- [ ] 변경 사항이 기존 아키텍처/도메인/테스트 컨벤션과 어긋나지 않는가?

