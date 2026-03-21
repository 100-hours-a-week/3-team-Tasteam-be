# Tasteam Backend

Tasteam 백엔드 애플리케이션 저장소입니다.

실행 코드와 설정은 이 레포를 기준으로 관리하고, 설계·운영·트러블슈팅 문서는 `BE.wiki`를 기준으로 봅니다.

## 바로가기

- [BE Wiki Home](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki)
- [Tasteam Wiki Home](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki)
- [Backend Workspace](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/Backend-Workspace)
- [문서 이관 현황](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EB%AC%B8%EC%84%9C-%EC%9D%B4%EA%B4%80-%ED%98%84%ED%99%A9)
- [검색 섹션 허브](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EA%B2%80%EC%83%89-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C)
- [멀티 모듈 섹션 허브](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EB%A9%80%ED%8B%B0-%EB%AA%A8%EB%93%88-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C)
- [비동기 아키텍처 섹션 허브](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EB%B9%84%EB%8F%99%EA%B8%B0-%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C)
- [모니터링 섹션 허브](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EB%AA%A8%EB%8B%88%ED%84%B0%EB%A7%81-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C)
- [트러블슈팅 로그](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BBE%5D-%ED%8A%B8%EB%9F%AC%EB%B8%94%EC%8A%88%ED%8C%85-%EB%A1%9C%EA%B7%B8)
- [docs 디렉터리 안내](docs/README.md)
- [부하 테스트 문서](loadtest/README.md)
- [모니터링 문서](monitoring/README.md)

## 스프린트

스프린트 | 기간 | 백엔드 개발 포커스 | 핵심 목적
-- | -- | -- | --
스프린트 1 | 2026.01.19 ~ 01.30 | 핵심 도메인 구축, 기본 API, 인증/권한 | MVP 동작 가능 상태
스프린트 2 | 2026.02.09 ~ 02.27 | 그룹 기반 추천 로직, 데이터 활용, 성능 개선 | 추천 서비스로서 가치 확보
스프린트 3 | 2026.03.09 ~ 03.20 | 개인화 추천, 반복 방지 로직, 운영 준비 | 완성도 및 확장성 확보

## 위키에서 먼저 볼 문서

| 구분 | 문서 | 링크 |
|---|---|---|
| 허브 | BE Wiki Home | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki) |
| 허브 | Backend Workspace | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/Backend-Workspace) |
| 허브 | 검색 섹션 허브 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EA%B2%80%EC%83%89-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C) |
| 허브 | 멀티 모듈 섹션 허브 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EB%A9%80%ED%8B%B0-%EB%AA%A8%EB%93%88-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C) |
| 허브 | 비동기 아키텍처 섹션 허브 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EB%B9%84%EB%8F%99%EA%B8%B0-%EC%95%84%ED%82%A4%ED%85%8D%EC%B2%98-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C) |
| 허브 | 모니터링 섹션 허브 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%EB%AA%A8%EB%8B%88%ED%84%B0%EB%A7%81-%EC%84%B9%EC%85%98-%ED%97%88%EB%B8%8C) |
| 테크 스펙 | 검색(Search) 도메인 테크 스펙 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BTech-Specs%5D-%EA%B2%80%EC%83%89%28Search%29-%EB%8F%84%EB%A9%94%EC%9D%B8-%ED%85%8C%ED%81%AC-%EC%8A%A4%ED%8E%99) |
| 테크 스펙 | 음식점(Restaurant) 테크 스펙 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BTech-Specs%5D-%EC%9D%8C%EC%8B%9D%EC%A0%90%28Restaurant%29-%ED%85%8C%ED%81%AC-%EC%8A%A4%ED%8E%99) |
| 테크 스펙 | 추천(Recommendation) 테크 스펙 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BTech-Specs%5D-%EC%B6%94%EC%B2%9C%28Recommendation%29-%ED%85%8C%ED%81%AC-%EC%8A%A4%ED%8E%99) |
| 트러블슈팅 | BE 트러블슈팅 로그 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BBE%5D-%ED%8A%B8%EB%9F%AC%EB%B8%94%EC%8A%88%ED%8C%85-%EB%A1%9C%EA%B7%B8) |
| 설계 | ERD 설계 컨벤션 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BBackend%5D-ERD-%EC%84%A4%EA%B3%84--%EA%B7%9C%EC%95%BD-%EB%B0%8F-%EC%BB%A8%EB%B2%A4%EC%85%98-%EB%AC%B8%EC%84%9C) |
| 설계 | API 명세서 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BBE-%E2%80%90-API%5D-API-%EB%AA%85%EC%84%B8%EC%84%9C) |
| 운영 | 비동기 이벤트드리븐 관측 운영 가이드 | [바로가기](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BRunbook%5D-%EB%B9%84%EB%8F%99%EA%B8%B0-%EC%9D%B4%EB%B2%A4%ED%8A%B8%EB%93%9C%EB%A6%AC%EB%B8%90-%EA%B4%80%EC%B8%A1-%EC%9A%B4%EC%98%81-%EA%B0%80%EC%9D%B4%EB%93%9C) |
| 배포 | 배포 (Docker, ECR, CodeDeploy, CI-CD) | [바로가기](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki/%EB%B0%B0%ED%8F%AC-%28Docker%2C-ECR%2C-CodeDeploy%2C-CI-CD%29) |

## 저장소 구조

| 경로 | 역할 |
|---|---|
| `module-app/api/` | 현재 Spring Boot 메인 애플리케이션과 API 진입점 |
| `app-admin/`, `app-batch/` | 관리자/배치 실행 자산 정리 공간 |
| `domain/`, `domain-core/`, `domain-rdb/` | 도메인 모델, 영속 계층, RDB 연계 코드 |
| `common/`, `common-infra/`, `common-security/`, `common-support/`, `common-web/` | 공통 웹/보안/인프라/지원 코드 |
| `docs/` | 레포 내부 설계, 컨벤션, 테스트 문서 |
| `monitoring/` | Prometheus, Grafana, Loki 등 관측 자산 |
| `loadtest/` | k6/Locust 기반 부하 테스트 시나리오와 시드 |
| `docker/`, `deploy/` | 로컬 실행 및 배포 스크립트/자산 |
| `scripts/`, `bin/` | 운영 및 개발 보조 스크립트 |

## 코드 품질 관리 도구

| 도구 | 적용 위치 | 용도 |
|---|---|---|
| Spotless | `build.gradle`, `.husky/pre-commit` | Java 포맷 강제 |
| Checkstyle | `build.gradle`, `config/checkstyle/*` | 네이버 룰 기반 스타일 검사 |
| JaCoCo | `build.gradle`, `module-app/api/build.gradle` | 테스트 커버리지 리포트와 최소 기준 검증 |
| SpotBugs + FindSecBugs | `module-app/api/build.gradle`, `.github/workflows/ci-cd-full.yml` | 잠재 버그와 보안 취약 패턴 정적 분석 |
| Java Test Fixtures | `module-app/api/build.gradle`, `src/testFixtures/java` | 공용 테스트 픽스처 재사용 |

### 로컬 점검 명령

```bash
./gradlew spotlessCheck
./gradlew checkstyleMain checkstyleTest
./gradlew test jacocoTestReport jacocoTestCoverageVerification
./gradlew :module-app:api:spotbugsMain
```

## 컨벤션과 참고 문서

- [Code 스타일 컨벤션](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BBE%5D-Code-%EC%8A%A4%ED%83%80%EC%9D%BC-%EC%BB%A8%EB%B2%A4%EC%85%98)
- [테스트 컨벤션 맵](https://github.com/100-hours-a-week/3-team-Tasteam-be/wiki/%5BBE%5D-%ED%85%8C%EC%8A%A4%ED%8A%B8-%EC%BB%A8%EB%B2%A4%EC%85%98-%EB%A7%B5)
- [Common 커밋 · 브랜치 컨벤션](https://github.com/100-hours-a-week/3-team-tasteam-wiki/wiki/%5BCommon%5D-%EC%BB%A4%EB%B0%8B-%C2%B7-%EB%B8%8C%EB%9E%9C%EC%B9%98-%EC%BB%A8%EB%B2%A4%EC%85%98)
- [docs 디렉터리 안내](docs/README.md)
- [테스트 문서 허브](docs/test/README.md)

## 프로젝트 기여

| Sei.Jang | Devon.woo | Gayeon Lee |
|---|---|---|
| <img src="https://github.com/Y0unse0.png" width="128" alt="Y0unse0" /> | <img src="https://github.com/ImGdevel.png" width="128" alt="ImGdevel" /> | <img src="https://github.com/GY102912.png" width="128" alt="GY102912" /> |
| [@Y0unse0](https://github.com/Y0unse0) | [@ImGdevel](https://github.com/ImGdevel) | [@GY102912](https://github.com/GY102912) |
| Backend | Backend | Backend |
