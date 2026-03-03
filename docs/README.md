# docs 디렉터리 안내

이 문서는 `docs/` 디렉터리 아래에 어떤 문서와 경로가 있는지, 그 안에 어떤 세부 문서가 포함되어 있는지를 정리합니다.

`docs/`에는 현재 `convention/`, `policy/` 하위가 존재합니다. 이 안내 문서는 각 디렉터리와 그 하위 구조를 설명하고, 추가적인 문서가 필요할 때 어디에 추가해야 하는지 알려줍니다.

## 최상위 구조

- `config/`: 빌드/정적 분석/포맷팅 설정 파일을 모아둔 공간입니다.
  - `config/formatter/naver-eclipse-formatter.xml`: 포맷터(Eclipse formatter) 설정 파일입니다. Spotless에서 사용합니다.
  - `config/checkstyle/naver-checkstyle-rules.xml`: Checkstyle 규칙 파일입니다. Gradle Checkstyle에서 사용합니다.
- `docs/convention/`: 팀의 코딩 컨벤션과 개발 규칙을 모아둔 공간입니다.
  - `docs/convention/커밋 브랜치 전략/`
    - `브랜치_커밋_전략.md`: 브랜치 이름, 커밋 메시지, PR 흐름 등 Git 워크플로우 전략을 문서화한 문서입니다.
  - `docs/convention/코드 스타일/`
    - `코드_스타일_컨벤션.md`: 자바/스프링 프로젝트에서 따르는 코드 스타일과 패턴을 정리한 문서입니다.
  - `docs/convention/flyway/`
    - `README.md`: Flyway 파일명/작성 규칙, 마이그레이션 설계 원칙을 정리한 문서입니다.
- `docs/policy/`: 운영/빌드/의존성 등 정책 문서를 모아둔 공간입니다.
  - `dependency-versions.md`: 의존성 버전 선택 이유 문서입니다.
  - `logging-policy.md`: 로그 저장/압축 정책 문서입니다.
- `docs/spec/`: 설계 산출물(ERD, API 명세)을 모아둔 공간입니다.
  - `docs/spec/erd/`: ERD 파일 저장
  - `docs/spec/api/`: API 명세서 저장
  - `docs/spec/tech/`: 도메인별 테크 스펙 문서 저장
    - `docs/spec/tech/main/README.md`: 메인/홈/AI 추천 화면 API의 설계 기준 문서입니다.
    - `docs/spec/tech/event/README.md`: 이벤트/공지 조회 API와 데이터 모델 설계 문서입니다.
    - `docs/spec/tech/bootstrap/README.md`: 로컬/개발 초기 데이터 부트스트랩 설계 문서입니다.
    - `docs/spec/tech/flyway/README.md`: Flyway 사용 방법과 운영/배포 절차를 정리한 문서입니다.
    - `docs/spec/tech/user-activity/README.md`: 사용자 이벤트 수집 모듈의 현재 구현 구조/흐름/설정 계약 문서입니다.
    - `docs/spec/tech/user-activity/RUNBOOK.md`: 사용자 이벤트 수집 장애 탐지/조사/복구 운영 런북 문서입니다.
    - `docs/spec/tech/user-activity/TRACEABILITY.md`: 이슈/PR/커밋/코드/테스트 추적성과 갭 리포트 문서입니다.

## 확장 포인트

현재 `docs/`는 위의 `convention/`, `policy/` 디렉터리를 포함하고 있습니다. 기능, 배포, 아키텍처 등 추가 문서가 필요해지면 이README를 참고하여 새로운 하위 디렉터리를 만들고 이 목록에 추가하시면 됩니다.

## 개발 툴 & Git 후크

- `package.json`: Husky를 설치하고 `prepare` 스크립트를 통해 Git 후크 초기화를 자동화합니다.
- `.husky/pre-commit`: 커밋 전에 `spotlessApply`(포맷팅) → `checkstyleMain/checkstyleTest`(규칙 검사)를 실행합니다(현재 테스트는 Docker 환경이 필요해 후크에서 제외합니다).
- `.husky/commit-msg`: `docs/convention/커밋 브랜치 전략/브랜치_커밋_전략.md`의 커밋 메시지 규칙(타입, 이모지 등)을 자동으로 적용하며, 잘못된 형식이나 `Co-authored-by` 트레일러가 있을 경우 커밋을 막습니다.
