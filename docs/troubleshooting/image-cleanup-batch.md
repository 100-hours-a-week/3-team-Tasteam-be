# 이미지 정리 배치 작업

## 개요
PENDING 상태로 만료된 이미지와 삭제 예정 이미지를 S3에서 삭제하는 배치 작업.

## 정리 대상

### 1. 만료된 PENDING 이미지 (도메인 미연결)
- Presigned URL 발급 후 도메인에 연결되지 않은 이미지
- `created_at`이 TTL(기본 24시간)보다 오래된 PENDING 상태 이미지
- 첫 정리 실행 시 `deleted_at`이 마킹되고, TTL 경과 후 실제 삭제

### 2. 삭제 마킹된 이미지 (최적화 후 원본)
- 이미지 최적화 시 원본 이미지에 `deleted_at`이 설정됨
- `deleted_at`으로부터 TTL이 지난 후 실제 삭제

## TTL 기반 삭제 정책

```
[이미지 생성] ──(24시간)──> [deletedAt 마킹] ──(24시간)──> [실제 삭제]
```

**TTL 대기 이유:**
- 업로드 진행 중인 이미지가 즉시 삭제되는 것을 방지
- 최적화 실패 시 원본 이미지 복구 가능 시간 확보
- 장애 복구를 위한 안전 마진

## 설정

```yaml
tasteam:
  file:
    cleanup:
      ttl-seconds: 86400  # 24시간 (기본값)
```

| 설정 키 | 환경변수 | 기본값 | 설명 |
|---------|----------|--------|------|
| `tasteam.file.cleanup.ttl-seconds` | `FILE_CLEANUP_TTL_SECONDS` | `86400` | 삭제 대기 시간 (초) |

## 정리 프로세스

### 1단계: 만료 PENDING 이미지 마킹
```java
// createdAt이 TTL보다 오래된 PENDING 이미지 (deletedAt 없음)
List<Image> expired = imageRepository
    .findAllByStatusAndDeletedAtIsNullAndCreatedAtBefore(PENDING, cutoff);

for (Image image : expired) {
    image.markDeletedAt(Instant.now());  // deletedAt 설정
}
```

### 2단계: TTL 경과 이미지 삭제
```java
// deletedAt이 TTL보다 오래된 PENDING 이미지
List<Image> targets = imageRepository
    .findAllByStatusAndDeletedAtBefore(PENDING, cutoff);

for (Image image : targets) {
    storageClient.deleteObject(image.getStorageKey());  // S3 삭제
    image.cleanup();  // status = DELETED
}
```

## 상태 전이

```
PENDING (created_at 오래됨, deletedAt=null)
    │
    ▼ [마킹]
PENDING (deletedAt 설정됨)
    │
    ▼ [TTL 경과 후]
DELETED (S3 삭제 완료)
```

## Admin API

### 정리 대상 조회
```
GET /api/v1/admin/jobs/image-cleanup/pending
```

**응답:**
```json
{
  "data": [
    {
      "imageId": 123,
      "fileName": "image.jpg",
      "fileSize": 102400,
      "fileType": "image/jpeg",
      "purpose": "REVIEW_IMAGE",
      "status": "PENDING",
      "createdAt": "2026-02-08T10:00:00Z",
      "deletedAt": "2026-02-09T03:00:00Z"
    }
  ]
}
```

### 정리 실행
```
POST /api/v1/admin/jobs/image-cleanup
```

**응답:**
```json
{
  "data": {
    "jobName": "image-cleanup",
    "successCount": 15,
    "failedCount": 0,
    "skippedCount": 0
  }
}
```

## 모니터링

### 로그 확인
```bash
# 정리 시작/완료
grep "Image cleanup" logs/*/info_*.log

# 삭제된 이미지
grep "Cleanup image" logs/*/info_*.log
```

### DB 조회
```sql
-- 정리 대기 이미지 (마킹됨, TTL 미경과)
SELECT * FROM image
WHERE status = 'PENDING'
AND deleted_at IS NOT NULL
AND deleted_at > NOW() - INTERVAL '24 hours';

-- 정리 대상 이미지 (TTL 경과)
SELECT * FROM image
WHERE status = 'PENDING'
AND (
    (deleted_at IS NOT NULL AND deleted_at < NOW() - INTERVAL '24 hours')
    OR (deleted_at IS NULL AND created_at < NOW() - INTERVAL '24 hours')
);

-- 삭제 완료된 이미지 통계
SELECT DATE(updated_at), COUNT(*)
FROM image
WHERE status = 'DELETED'
GROUP BY DATE(updated_at)
ORDER BY 1 DESC;
```

## 트러블슈팅

### 이미지가 즉시 삭제되지 않음
**원인:** TTL 대기 정책으로 인해 `deletedAt` 설정 후 TTL 경과 후에 삭제됨.

**확인:**
```sql
SELECT id, deleted_at,
       deleted_at + INTERVAL '24 hours' AS expected_cleanup_time
FROM image
WHERE status = 'PENDING' AND deleted_at IS NOT NULL;
```

### 업로드 중인 이미지가 삭제됨
**가능성 낮음:** TTL이 24시간이므로 정상 업로드 과정에서는 발생하지 않음.

**예방:**
- 클라이언트에서 Presigned URL 발급 후 즉시 업로드 및 도메인 연결
- 도메인 연결 시 `ACTIVE` 상태로 전환되어 정리 대상에서 제외

### S3 삭제 실패
**원인:** 네트워크 오류, 권한 문제 등

**대응:**
- 개별 이미지 실패는 로깅 후 다음 이미지 처리 계속
- 실패한 이미지는 다음 정리 실행 시 재시도
- 반복 실패 시 수동 확인 필요

## 관련 문서
- [File 모듈 테크 스펙](../spec/tech/file/README.md)
- [이미지 최적화 배치 작업](./image-optimization-batch.md)
- [이미지 정책](../policy/image-policy.md)
