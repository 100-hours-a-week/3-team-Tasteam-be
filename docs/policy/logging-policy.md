# 로그 파일 정책

이 문서는 `app-api/src/main/resources/logback-spring.xml` 기준의 로그 저장/압축 정책을 설명합니다.

## 기본 경로

- 기본 로그 경로: `./logs`
  - 애플리케이션 실행 위치 기준의 상대 경로
  - 로컬 실행 시: 프로젝트 루트 기준 `./logs/`
  - Docker 실행 시: 컨테이너 작업 디렉터리(`/app`) 기준 `./logs/` → 실제 경로는 `/app/logs`

## 콘솔 로그

- 출력 레벨: `INFO` 이상 (prod는 `WARN` 이상)
- 포맷: UTC 타임스탬프 + 컬러 레벨 + 스레드 + PID + 로거

## 파일 로그

- 저장 레벨: `DEBUG` 이상 (prod는 `WARN` 이상)
- 롤링 정책: 날짜 + 용량 동시 적용
  - 디렉터리: 날짜 단위로 분리
  - 파일명 패턴: `./logs/YYYY-MM-DD/info_YYYY-MM-DD.N.log.gz`
  - 용량 제한: 파일당 10MB 초과 시 `N` 증가
  - 압축: `.gz`로 압축 저장
  - 보관 기간: 30일 (`maxHistory=30`)

예시:

- `./logs/2026-01-19/info_2026-01-19.0.log.gz`
- `./logs/2026-01-19/info_2026-01-19.1.log.gz` (10MB 초과 시)

## Docker 사용 시 참고

- 컨테이너 내부 저장 경로: `/app/logs`
- 컨테이너 삭제 시 로그가 유실됩니다. 지속 보관이 필요하면
  Docker Compose에 볼륨 마운트를 추가하세요.

예시(선택):

```yaml
services:
  api:
    volumes:
      - ./logs:/app/logs
```

## 타임존

- 로그 타임스탬프는 `UTC` 기준으로 출력됩니다.
