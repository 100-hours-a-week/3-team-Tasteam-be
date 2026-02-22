# CI/CD 전략 (v2)

이 문서는 현재 `infra/#363/deploy` 브랜치 기준 v2의 CI/CD 전략을 정리합니다.

---

## 개요

현재 두 가지 배포 파이프라인이 공존합니다.

| 구분 | Pipeline 1 (Legacy) | Pipeline 2 (v2 – CodeDeploy) |
|------|---------------------|------------------------------|
| 워크플로우 | `ci-cd-full.yml` | `codedeploy-dev-test.yml` |
| 빌드 산출물 | JAR (bootJar) | Docker 이미지 (ECR) |
| 배포 방식 | SCP + SSH → 서버 스크립트 | S3 + AWS CodeDeploy |
| 인프라 접근 | SSH 키 기반 | OIDC 기반 AWS 인증 |
| 트리거 | `develop`/`main` push | `deploy/codedeploy/backend/**` 변경 또는 수동 |

---

## 1. Pipeline 1 – Legacy JAR 기반 (`ci-cd-full.yml`)

### CI (PR 시)

| 단계 | 설명 |
|------|------|
| **테스트** | `./gradlew test` |
| **보안 분석** | CodeQL (`security-and-quality`) + SpotBugs (`spotbugsMain`) |

- 실패 시 Discord 웹훅 알림

### CD (push 시 – `develop`/`main`)

```
bootJar 빌드 → GitHub Artifact 업로드
  → SCP로 서버 전송 → SSH로 deploy.sh 실행 (Blue-Green)
```

- `environment`: `develop` → `development`, `main` → `production`
- 롤백: `rollback.yml` (수동 `workflow_dispatch`, Run Number 입력)
- 배포·롤백 결과 Discord 알림

---

## 2. Pipeline 2 – Container 기반 CodeDeploy (`codedeploy-dev-test.yml`)

### 빌드

1. OIDC로 AWS 인증
2. ECR 로그인
3. Docker Buildx 멀티스테이지 빌드 (`app-api/Dockerfile`)
   - `gradle:9.2.1-jdk21` → `amazoncorretto:21`
   - GitHub Actions 캐시 (`type=gha`)
4. ECR에 `<sha>` 태그 + `latest` 태그 푸시

### 배포

1. CodeDeploy 아티팩트 패키징 (`appspec.yml` + 셸 스크립트 + docker-compose + `.env.deploy`)
2. S3에 zip 업로드
3. `aws deploy create-deployment` → CodeDeploy가 EC2에서 실행

### 서버 측 배포 스크립트 (`deploy/codedeploy/backend/deploy.sh`)

```
ApplicationStop  → stop.sh (기존 컨테이너 제거)
AfterInstall     → deploy.sh deploy
                   ├─ SSM Parameter Store에서 환경변수 가져오기
                   ├─ (dev 환경) Postgres/Redis 컨테이너 시작
                   ├─ ECR 로그인 → 이미지 pull
                   ├─ Docker Compose up (backend 서비스)
                   └─ 환경변수 검증 (DB_URL, REDIS_HOST 등)
ValidateService  → health.sh (actuator/health 체크, 최대 45회 × 2초)
```

### 트래픽 전환 (`traffic.sh`)

- **Caddy 기반 Blue/Green**
  - `BLUE_UPSTREAM` / `GREEN_UPSTREAM` 설정
  - Caddyfile의 upstream을 sed로 교체 → `caddy validate` → `caddy reload`
  - 전환 실패 시 자동 롤백 (백업 Caddyfile 복원)
  - `switch.sh` / `rollback.sh`으로 간편 호출

---

## 3. 보조 워크플로우

| 워크플로우 | 트리거 | 역할 |
|-----------|--------|------|
| `ci-quick-check.yml` | `feat/**`, `hotfix/**` push | 빌드만 확인 (`build -x test`), 실패 시 Discord 알림 |
| `pr-review-gate.yml` | PR opened/ready/reopened | Assignee 자동 할당 + `BE` 라벨 부착 + 리뷰어 요청 |
| `security-daily.yml` | 매일 03:00 KST (cron) | CodeQL `security-extended` + `security-and-quality` 정밀 분석 |
| `add-issue-to-project.yml` | 이슈 생성 | `기능`/`버그` 라벨 이슈를 Projects v2에 자동 추가 |

---

## 4. 환경 구성

### 개발 환경 (dev)

- EC2에서 DB(PostGIS)/Redis를 Docker Compose로 함께 운영 (`docker-compose.dev-infra.yml`)
- SSM Parameter Store 경로: `/dev/tasteam/backend`

### 운영 환경 (prod)

- 외부 관리형 DB/Redis 사용 (SSM에서 접속 정보 가져옴)
- SSM Parameter Store 경로: `/prod/tasteam/backend` (추정)

### 로컬 개발

- `docker-compose.local.yml` – PostGIS + Redis + API 서비스
- `.env.local`로 환경변수 관리

---

## 5. v1 → v2 전환 요약

| 항목 | v1 (Legacy) | v2 (CodeDeploy) |
|------|-------------|-----------------|
| 빌드 산출물 | JAR 파일 | Docker 이미지 |
| 배포 트리거 | develop/main push | 배포 스크립트 변경 또는 수동 |
| 서버 접근 | SSH 키 | AWS OIDC + CodeDeploy Agent |
| 환경변수 | 서버에 직접 설정 | SSM Parameter Store |
| 인프라 코드 | 없음 | appspec.yml + docker-compose |
| 이미지 저장소 | 없음 | Amazon ECR |
| 캐시 | Gradle 캐시만 | Gradle + Docker layer 캐시 (GHA) |
| 트래픽 전환 | deploy.sh 내장 | Caddy Blue/Green (`traffic.sh`) |
