# 예외 패키지

이 패키지는 애플리케이션 전반의 예외 처리 흐름을 담당한다.

## 구성 요소

- ErrorCode: 도메인/시스템 에러 코드의 공통 인터페이스
- code: ErrorCode 구현(공통 에러 코드 등)
- business: 비즈니스 예외 정의
- handler: 전역 예외 처리(ControllerAdvice)

## 사용 흐름

1. 도메인/서비스 계층에서 ErrorCode 기반으로 예외를 생성한다.
2. GlobalExceptionHandler가 예외를 받아 공통 에러 응답을 만든다.
3. 응답 포맷은 ErrorResponse를 통해 일관되게 유지한다.
