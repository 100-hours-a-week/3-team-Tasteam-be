# 로컬 모니터링 스택

이 디렉토리는 백엔드 로컬 개발용 `Alloy + Prometheus + Loki + Grafana + Tempo` 자산을 모아둔 위치다.
`docker-compose.local-monitoring.yml`에서만 참조되며, staging/production 배포 경로에는 포함되지 않는다.

## 실행

```bash
docker compose -f docker-compose.local.yml -f docker-compose.local-monitoring.yml up -d --build
```

포트 충돌이 있으면 아래처럼 호스트 포트를 바꿔 실행한다.

```bash
MONITORING_PROMETHEUS_PORT=19090 \
MONITORING_GRAFANA_PORT=13001 \
docker compose -f docker-compose.local.yml -f docker-compose.local-monitoring.yml up -d --build
```

## 접속 주소

- Grafana: `http://localhost:3001`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100`
- Tempo: `http://localhost:3200`
- Alloy metrics: `http://localhost:12345/metrics`

## API 실행 방식별 메트릭 타깃

- 기본값: `SPRING_METRICS_TARGET=api:8080`
  - `docker compose`의 `api` 서비스를 함께 올릴 때 사용한다.
- 대체값: `SPRING_METRICS_TARGET=host.docker.internal:8080`
  - `./gradlew bootRun`으로 API를 직접 띄울 때 사용한다.

## 주요 파일

- `alloy/config.alloy`: Spring Actuator scrape + Prometheus remote_write + Docker logs → Loki
- `prometheus/prometheus.yml`: Prometheus 수신기, infra exporter scrape, recording rule 로딩
- `loki/loki.yml`: 로컬 Loki 저장소 설정
- `grafana/provisioning/`: datasource / dashboard 자동 프로비저닝
- `grafana/dashboards/`: 카테고리별로 분리한 Grafana 대시보드 JSON

## Grafana 폴더 구조

- `grafana/dashboards/application`: `Tasteam Application`
- `grafana/dashboards/infrastructure`: `Tasteam Infrastructure`
- `grafana/dashboards/logs-tracing`: `Tasteam Logs & Tracing`
- `grafana/dashboards/async-events`: `Tasteam Async & Events`

## 포함된 이관 대시보드

- `Tasteam Application`: Spring Core, Spring Cache
- `Tasteam Infrastructure`: Node Exporter, Redis, PostgreSQL RDS
- `Tasteam Logs & Tracing`: Spring Transaction Detail, Server Logs
- `Tasteam Async & Events`: Spring Async

현재 Spring 중심 운영 대시보드는 아래 4개로 축소했다.

- `Tasteam Spring Core`
- `Tasteam Spring Async`
- `Tasteam Spring Cache`
- `Tasteam Spring Transaction Detail`

## 참고

- `Redis`, `Node Exporter`, `Loki`, `CloudWatch` datasource까지 provisioning 했지만, `CloudWatch` 패널은 로컬 Grafana 컨테이너에 AWS 자격 증명이 있을 때만 실제 값을 조회한다.
- `RDS` 대시보드는 로컬 postgres-exporter 지표도 일부 재사용할 수 있지만, CloudWatch 기반 패널은 운영 자격 증명이 없으면 비어 있을 수 있다.

## 확인 포인트

```bash
curl -s localhost:8080/actuator/prometheus | rg 'tasteam_|cache_|executor_|async_pipeline_'
curl -s localhost:9090/api/v1/label/job/values
curl -s localhost:3100/ready
```
