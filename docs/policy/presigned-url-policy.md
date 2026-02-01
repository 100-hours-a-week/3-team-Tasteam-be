# Presigned URL 정책 (이미지)

_최종 업데이트: 2026-02-01_

## 1. 적용 범위
- `Image` 업로드/조회에 사용하는 **S3 Presigned POST/GET** 정책 요약.
- 발급 엔드포인트: `POST /api/v1/files/uploads/presigned` (최대 5개 파일 메타를 한 번에 요청).
- 조회 엔드포인트: `GET /api/v1/files/{fileUuid}/url` (prod/dev 기본 `presigned` 모드로 GET URL 반환).

## 2. 업로드 Presigned POST 정책
- **스토리지 타입**: 기본 `s3` (prod/dev), 로컬 기본값은 `dummy`.
- **만료 시간**: 요청 시각 + `tasteam.storage.presigned-expiration-seconds` (기본 300초).
- **Policy 조건(고정)**
  - `bucket` = 설정된 버킷
  - `key` = 서버가 생성한 object key (아래 "경로 규칙")
  - `content-length-range` = [`minSizeBytes`, `maxSizeBytes`]
  - `Content-Type` = 요청한 MIME 타입(allowlist 통과 값)
  - `x-amz-algorithm` = `AWS4-HMAC-SHA256`
  - `x-amz-credential`, `x-amz-date`, (`x-amz-security-token` if STS)
- **폼 필드**: 위 조건을 만족하는 `policy`/`signature`/`credential`/`date`/`Content-Type`/`key` 등을 응답 `fields`로 반환.
- **요청 검증(서버 측)**
  - `fileName` 필수, 256자 이하.
  - `contentType` 필수, 허용 목록: `image/jpeg`, `image/jpg`, `image/png`, `image/webp`.
  - `size` 필수, `minSizeBytes` 이상 `maxSizeBytes` 이하.
  - 1회 요청당 파일 목록은 1~5개.
- **S3 측 크기 상한 계산**: `content-length-range`의 상한은 `min(requested size, maxSizeBytes)`로 설정하여, 클라이언트가 선언한 크기보다 큰 업로드를 막음.
- **경로 규칙 (object key)**
  - `REVIEW_IMAGE` → `uploads/review/image/<uuid>.<ext>`
  - `RESTAURANT_IMAGE` → `uploads/restaurant/image/<uuid>.<ext>`
  - `PROFILE_IMAGE` → `uploads/profile/image/<uuid>.<ext>`
  - `GROUP_IMAGE` → `uploads/group/image/<uuid>.<ext>`
  - `COMMON_ASSET` → `uploads/common/asset/<uuid>.<ext>`
  - 목적 미지정(null) → `tasteam.storage.temp-upload-prefix` (기본 `uploads/temp`)
  - 확장자는 원본 파일명에서 추출하거나 MIME 타입 기반(`jpg|png|gif|webp`), 없으면 확장자 없이 key 생성.
- **상태 플로우**: Presigned 발급 시 `Image` row는 `PENDING`; 도메인 연결 시 `ACTIVE`, cleanup 시 `DELETED`.
- **로그/보안**: 정책 생성 시 서명 재료와 key는 로그에 마스킹(`StorageProperties.logConfiguration`); 서명 실패/권한 오류는 비즈니스 예외로 매핑.
- **Dummy 스토리지**: `tasteam.storage.type=dummy`일 때는 만료만 적용하고 단순 URL/빈 필드 반환(실제 업로드 없음).

## 3. 조회 Presigned GET 정책
- `tasteam.storage.access-mode=presigned`일 때 `buildPublicUrl`이 **GET Presigned URL**을 생성.
- 만료 시간: 업로드와 동일한 `presigned-expiration-seconds`.
- `access-mode=public`이면 `base-url + storageKey` 정적 URL을 반환(서명 없음).

## 4. 구성값(환경변수 매핑)
- `tasteam.storage.access-mode` (`STORAGE_ACCESS_MODE`) : `presigned`(prod/dev 기본) | `public`.
- `tasteam.storage.presigned-expiration-seconds` (`STORAGE_PRESIGNED_EXPIRATION_SECONDS`) : 기본 300.
- `tasteam.storage.temp-upload-prefix` (`STORAGE_TEMP_UPLOAD_PREFIX`) : 기본 `uploads/temp`.
- `tasteam.storage.base-url` (`STORAGE_BASE_URL`) : 정적 접근 시 사용.
- `tasteam.file.upload.min-size-bytes` (`FILE_UPLOAD_MIN_SIZE_BYTES`) : 기본 1 byte.
- `tasteam.file.upload.max-size-bytes` (`FILE_UPLOAD_MAX_SIZE_BYTES`) : 기본 10MB.
- `tasteam.file.upload.allowed-content-types` (`FILE_UPLOAD_ALLOWED_CONTENT_TYPES`) : 기본 `image/jpeg,image/jpg,image/png,image/webp`.
- `tasteam.file.cleanup.ttl-seconds` (`FILE_CLEANUP_TTL_SECONDS`) : PENDING 유지 TTL, 기본 86400초(24h).

## 5. 운영 메모
- PENDING 이미지는 `ttl-seconds` 초 경과 후 soft delete→batch에서 S3 오브젝트 삭제.
- GET/POST 모두 같은 만료값을 쓰므로, 만료를 늘릴 때는 업로드 폼 재사용 기간과 노출 기간을 함께 고려.
- 현재 코드에는 presigned 발급 rate limit은 구현되어 있지 않음(문서 내 언급만 존재); 필요 시 Redis 기반 throttling 추가 검토.
