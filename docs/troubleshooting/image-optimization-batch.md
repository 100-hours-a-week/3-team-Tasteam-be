# 이미지 최적화 배치 작업 트러블슈팅

## 개요
S3에 저장된 기존 이미지를 주기적으로 스캔하여 정책에 맞게 최적화(리사이즈, WebP 변환)하는 배치 작업.

## 이미지 정책
| 유형 | 최대 크기 | 비율 | 크롭 |
|------|----------|------|------|
| 프로필/그룹 | 100×100px | 1:1 | 중앙 크롭 |
| 음식점 | 너비 2048px | 유지 | 금지 |
| 리뷰/메뉴 | 너비 768px | 유지 | 금지 |

## 설정
```yaml
tasteam:
  image:
    optimization:
      enabled: true  # 기본값 false
      cron: "0 0 3 * * ?"  # 매일 새벽 3시
      batch-size: 100
```

## 점검 포인트 및 해결책

### 1. S3 커넥션 누수
**문제**: S3Object 스트림을 닫지 않으면 HTTP 커넥션이 풀로 반환되지 않아 커넥션 풀 고갈 발생.

**증상**:
- `Connection pool shut down` 에러
- S3 요청 타임아웃 증가
- 배치 작업 중단

**해결**: try-with-resources로 S3Object 자동 해제
```java
try (S3Object s3Object = amazonS3.getObject(bucket, key)) {
    return IOUtils.toByteArray(s3Object.getObjectContent());
}
```

### 2. 트랜잭션 장시간 점유
**문제**: 전체 배치를 하나의 트랜잭션으로 처리 시 DB 커넥션 장시간 점유.

**증상**:
- DB 커넥션 풀 고갈
- 다른 API 요청 지연
- 트랜잭션 타임아웃

**해결**: 이미지별 개별 트랜잭션으로 분리
```java
// 배치 메서드에서 @Transactional 제거
public OptimizationResult processOptimizationBatch(int batchSize) {
    List<Image> images = findUnoptimizedImages(batchSize);
    for (Image image : images) {
        processSingleImage(image);  // 개별 트랜잭션
    }
}

@Transactional
public OptimizationOutcome processSingleImage(Image image) { ... }
```

### 3. 메모리 부족 (OOM)
**문제**: 대용량 이미지를 byte[]로 메모리에 로드 시 OOM 발생 가능.

**증상**:
- `OutOfMemoryError: Java heap space`
- GC 오버헤드 증가

**대응**:
- `batch-size` 값 축소 (기본값 100 → 필요시 50 이하)
- 힙 메모리 증설
- 이미지 최대 크기 제한 (현재 10MB)

### 4. 중복 실행 방지
**문제**: 스케줄러가 이전 작업 완료 전에 다시 실행될 수 있음.

**해결**: AtomicBoolean으로 실행 상태 관리
```java
private final AtomicBoolean isRunning = new AtomicBoolean(false);

public void runOptimization() {
    if (!isRunning.compareAndSet(false, true)) {
        log.warn("Job already running, skipping");
        return;
    }
    try {
        // 작업 수행
    } finally {
        isRunning.set(false);
    }
}
```

### 5. 개별 이미지 실패 격리
**문제**: 하나의 이미지 실패가 전체 배치에 영향을 주면 안 됨.

**해결**: 개별 이미지 처리를 try-catch로 감싸고 실패 기록
```java
for (Image image : images) {
    try {
        processSingleImage(image);
    } catch (Exception e) {
        log.error("Failed: {}", image.getId());
        failedCount++;
    }
}
```

## 모니터링

### 로그 확인
```bash
# 배치 시작/완료 로그
grep "Image optimization" logs/*/info_*.log

# 실패 건 확인
grep "Failed to optimize" logs/*/error_*.log
```

### DB 조회
```sql
-- 상태별 통계
SELECT status, COUNT(*)
FROM image_optimization_job
GROUP BY status;

-- 최근 실패 건
SELECT * FROM image_optimization_job
WHERE status = 'FAILED'
ORDER BY processed_at DESC
LIMIT 10;

-- 미처리 이미지 수
SELECT COUNT(*) FROM image i
WHERE i.status = 'ACTIVE'
AND i.id NOT IN (SELECT image_id FROM image_optimization_job);
```

## 수동 실행
배치를 수동으로 트리거하려면 서비스 메서드 직접 호출:
```java
@Autowired
ImageOptimizationService service;

service.processOptimizationBatch(50);  // 50개씩 처리
```

## 롤백
S3 버전닝이 활성화되어 있으면 원본 이미지 복구 가능:
```bash
aws s3api list-object-versions --bucket {bucket} --prefix {key}
aws s3api get-object --bucket {bucket} --key {key} --version-id {version-id} restored.webp
```
