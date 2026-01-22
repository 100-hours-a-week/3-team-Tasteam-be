# PostGIS 확장 + 검색 테이블 세팅

## 문제
- `ERROR: type "geometry" does not exist`
- `ERROR: relation "restaurant" does not exist`

검색 도메인에서는 PostGIS가 활성화된 PostgreSQL과 `restaurant`, `member_serach_history` 같은 테이블이 필요하지만, Docker Compose로 띄운 `postgres:17`에는 PostGIS가 기본 포함되어 있지 않고 데이터베이스가 비어 있습니다.

## 해결 절차
1. 로컬 PostgreSQL 컨테이너에 접속
   ```bash
   docker-compose -f docker-compose.local.yml exec db psql -U tasteam -d tasteam
   ```
2. PostGIS 확장 활성화
   ```sql
   CREATE EXTENSION IF NOT EXISTS postgis;
   ```
3. 테이블 생성
   - `app-api`에서 사용하는 마이그레이션(Flyway/Liquibase 또는 SQL) 명령을 실행하거나, 수동 SQL(DDL)을 먼저 적용해서 `restaurant`, `member_serach_history` 등 테이블을 생성합니다.
   - 예: `./gradlew -p app-api flywayMigrate` (프로젝트에 flyway 설정이 있는 경우)
4. 애플리케이션 재시작
   - PostGIS와 테이블이 준비된 상태에서 `spring.jpa.database-platform=org.hibernate.spatial.dialect.postgis.PostgisDialect`를 사용하는 API를 기동합니다.

## 참고
- `docker-compose.local.yml`의 DB는 외부 포트 `5432`로 노출되므로, 직접 `psql`로 접속하거나 DBeaver 등 툴로 접속해도 무방합니다.
- 이 과정을 `README.md`나 운영 문서에 기록해두면 새로운 개발자가 동일한 문제를 피할 수 있습니다.
