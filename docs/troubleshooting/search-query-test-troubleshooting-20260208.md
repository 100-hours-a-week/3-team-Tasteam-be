# SearchQueryRepositoryTest 트러블슈팅 로그 - 2026-02-08

## 1) `%` 연산자 오류 (mod)
- **증상**: `FunctionArgumentException: Parameter 1 of function 'mod()' has type 'INTEGER'`
- **원인**: HQL에서 `%`가 `pg_trgm`가 아니라 **mod 연산**으로 해석됨
- **조치**: `lower(name) % keyword` 제거 → `similarity(name, keyword) >= 0.3`로 대체
- **파일**: `app-api/src/main/java/com/tasteam/domain/search/repository/impl/SearchQueryRepositoryImpl.java`

## 2) `+` 연산 타입 오류
- **증상**: `SemanticException: Operand of + is of type 'java.lang.Object'`
- **원인**: `similarity()/greatest()` 결과 타입이 Hibernate에서 `Object`로 추론
- **조치**: `cast(function('similarity', ...) as double)` + `cast(greatest(...) as double)`로 명시 캐스팅
- **파일**: `app-api/src/main/java/com/tasteam/domain/search/repository/impl/SearchQueryRepositoryImpl.java`

## 3) `-` 연산 타입 오류
- **증상**: `SemanticException: Operand of - is of type 'java.lang.Object'`
- **원인**: `1.0 - (distance / radius)`에서 하위 타입 추론 실패
- **조치**: 분자/분모를 `cast(... as double)`로 명시 캐스팅
- **파일**: `app-api/src/main/java/com/tasteam/domain/search/repository/impl/SearchQueryRepositoryImpl.java`

## 4) 재실행 결과
- **테스트**: `./gradlew :app-api:test --tests com.tasteam.domain.search.repository.SearchQueryRepositoryTest`
- **결과**: BUILD SUCCESSFUL
