# docs 디렉터리 안내

이 문서는 `docs/` 디렉터리 아래에 어떤 문서와 경로가 있는지, 그 안에 어떤 세부 문서가 포함되어 있는지를 정리합니다.

`docs/`에는 현재 `convention/`, `policy/` 하위가 존재합니다. 이 안내 문서는 각 디렉터리와 그 하위 구조를 설명하고, 추가적인 문서가 필요할 때 어디에 추가해야 하는지 알려줍니다.

## 최상위 구조

- `docs/convention/`: 팀의 코딩 컨벤션과 개발 규칙을 모아둔 공간입니다.
  - `docs/convention/인텔리제이_포맷터/`
    - `naver-eclipse-formatter.xml`: 네이버 IDE 설정 기반의 포맷터 설정을 담고 있으며, 프로젝트에서 사용할 포맷터 XML을 보관합니다.
  - `docs/convention/커밋 브랜치 전략/`
    - `브랜치_커밋_전략.md`: 브랜치 이름, 커밋 메시지, PR 흐름 등 Git 워크플로우 전략을 문서화한 문서입니다.
  - `docs/convention/코드 스타일/`
    - `코드_스타일_컨벤션.md`: 자바/스프링 프로젝트에서 따르는 코드 스타일과 패턴을 정리한 문서입니다.
- `docs/policy/`: 운영/빌드/의존성 등 정책 문서를 모아둔 공간입니다.
  - `dependency-versions.md`: 의존성 버전 선택 이유 문서입니다.
  - `logging-policy.md`: 로그 저장/압축 정책 문서입니다.
- `docs/spec/`: 설계 산출물(ERD, API 명세)을 모아둔 공간입니다.
  - `docs/spec/erd/`: ERD 파일 저장
  - `docs/spec/api/`: API 명세서 저장
  - `docs/spec/tech/`: 도메인별 테크 스펙 문서 저장

## 확장 포인트

현재 `docs/`는 위의 `convention/`, `policy/` 디렉터리를 포함하고 있습니다. 기능, 배포, 아키텍처 등 추가 문서가 필요해지면 이README를 참고하여 새로운 하위 디렉터리를 만들고 이 목록에 추가하시면 됩니다.

## 개발 툴 & Git 후크

- `package.json`: Husky를 설치하고 `prepare` 스크립트를 통해 Git 후크 초기화를 자동화합니다.
- `.husky/pre-commit`: 커밋 전에 `./gradlew spotlessCheck check -x test`를 실행해 Spotless 검사와 컴파일/정적 검사를 연달아 확인합니다(현재 테스트는 Docker 환경이 필요해 후크에서 제외합니다).
- `.husky/commit-msg`: `docs/convention/커밋 브랜치 전략/브랜치_커밋_전략.md`의 커밋 메시지 규칙(타입, 이모지 등)을 자동으로 적용하며, 잘못된 형식이나 `Co-authored-by` 트레일러가 있을 경우 커밋을 막습니다.
