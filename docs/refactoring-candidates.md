# 리팩토링 개선 후보 목록

작성일: 2026-02-05

아래 목록은 현재 코드베이스를 빠르게 스캔하면서 발견한 개선 후보입니다. 실제 작업 전에 담당 기능의 테스트/운영 영향도를 확인한 뒤 우선순위를 확정하세요.

## 후보 목록

| 우선순위 | 영역 | 파일/위치 | 관찰된 문제 | 제안 방향 |
| --- | --- | --- | --- | --- |
| P1 | 도메인 서비스 비대화 | `app-api/src/main/java/com/tasteam/domain/restaurant/service/RestaurantService.java` | 600+ LOC, 조회/검색/이미지 처리/AI 요약/스케줄 로직이 한 클래스에 집중됨 | 읽기 전용/쓰기 전용 서비스 분리, 이미지/AI 요약/스케줄 로직은 전용 서비스로 분리, 응답 조립은 mapper로 분리 |
| P1 | 도메인 서비스 비대화 | `app-api/src/main/java/com/tasteam/domain/subgroup/service/SubgroupService.java` | 500+ LOC, 가입/인증/검색/페이징/이미지 처리 혼재 | 가입/인증 서비스 분리, 이미지 처리 전용 서비스 분리, 커서 페이지 빌더 공통화 |
| P1 | 도메인 서비스 비대화 | `app-api/src/main/java/com/tasteam/domain/group/service/GroupService.java` | 400+ LOC, 그룹 CRUD/이메일 인증/멤버 관리/이미지 처리 혼재 | 그룹 CRUD, 그룹 인증, 그룹 멤버, 이미지 처리로 분리 |
| P1 | 이미지 연결 로직 중복 | `app-api/src/main/java/com/tasteam/domain/restaurant/service/RestaurantService.java` `app-api/src/main/java/com/tasteam/domain/review/service/ReviewService.java` `app-api/src/main/java/com/tasteam/domain/group/service/GroupService.java` `app-api/src/main/java/com/tasteam/domain/subgroup/service/SubgroupService.java` | DomainImage 생성/정렬/활성화 로직이 여러 서비스에 중복됨 | `DomainImageLinker` 또는 `DomainImageService`로 공통화, 검증/예외 규칙 일원화 |
| P1 | 테스트용 API 노출 | `app-api/src/main/java/com/tasteam/domain/test/controller/WebhookTestController.java` | main 소스에 테스트용 컨트롤러가 존재하며 @Profile 제한 없음 | dev/local 프로필로 제한하거나 test 모듈로 이동 |
| P2 | 파일 서비스 단일 책임 위반 | `app-api/src/main/java/com/tasteam/domain/file/service/FileService.java` | presigned 업로드/링크/조회/정책/정리 로직이 혼재됨 | 업로드/링크/조회/정리 서비스로 분리, 정책 검증은 별도 validator로 분리 |
| P2 | 리뷰 서비스 비대화 | `app-api/src/main/java/com/tasteam/domain/review/service/ReviewService.java` | 리뷰 CRUD/키워드/이미지/커서 페이징 혼재, @Component 사용 | @Service로 표준화, 키워드/이미지/리뷰 조회 분리 |
| P2 | 메인 페이지 조립 로직 중복 | `app-api/src/main/java/com/tasteam/domain/main/service/MainService.java` | 섹션 구성/이미지/카테고리/요약 조합이 메서드별 반복 | 섹션 빌더 유틸/전용 서비스로 중복 제거 |
| P2 | S3 서명 로직 복잡도 | `app-api/src/main/java/com/tasteam/infra/storage/s3/S3StorageClient.java` | 직접 SigV4 서명 생성, 예외 매핑/검증 로직이 한 클래스에 집중 | 서명/정책 생성 유틸 분리, 테스트 가능한 컴포넌트로 분리 |
| P2 | 커서 페이징 패턴 중복 | `app-api/src/main/java/com/tasteam/domain/restaurant/service/RestaurantService.java` `app-api/src/main/java/com/tasteam/domain/subgroup/service/SubgroupService.java` `app-api/src/main/java/com/tasteam/domain/review/service/ReviewService.java` | 커서 디코딩/유효성 검사/다음 커서 생성 로직이 곳곳에 존재 | 공통 `CursorPageBuilder`/`CursorValidator` 도입 |
| P3 | 레거시 엔티티/리포지토리 가능성 | `app-api/src/main/java/com/tasteam/domain/restaurant/entity/RestaurantImage.java` `app-api/src/main/java/com/tasteam/domain/restaurant/repository/RestaurantImageRepository.java` | 사용처가 보이지 않으며 엔티티에 “DomainImage 사용으로 대체됨” 주석 | 미사용 여부 확인 후 제거 또는 마이그레이션 문서화 |
| P3 | Swagger docs 중복 구조 | `app-api/src/main/java/com/tasteam/domain/**/controller/docs/*ControllerDocs.java` | 컨트롤러/문서 인터페이스 이중화로 유지보수 부담 | 메타 애노테이션 또는 컨트롤러 직접 애노테이션으로 단순화 |

## 추가 확인이 필요한 사항

- `domain/test` 패키지가 운영 환경에서 라우팅되는지 확인 필요
- `RestaurantImage` 관련 DB 마이그레이션/테이블 사용 여부 확인 필요

## 제안된 다음 단계

1. P1 후보 중 1~2개를 선택하여 분리 대상과 책임 범위를 합의
2. 분리된 컴포넌트별로 통합 테스트 기준 정의
3. 공통화 대상(이미지/커서) 먼저 유틸화 후 서비스별 적용

## 상세 예시 및 리팩토링 실행 계획

### 1. RestaurantService 비대화

```[코드]
@Transactional(readOnly = true)
public RestaurantDetailResponse getRestaurantDetail(long restaurantId) {
	Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
		.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

	List<String> foodCategories = restaurantFoodCategoryRepository.findByRestaurantId(restaurantId)
		.stream()
		.map(RestaurantFoodCategory::getFoodCategory)
		.map(FoodCategory::getName)
		.toList();

	List<BusinessHourWeekItem> businessHoursWeek = restaurantScheduleService.getBusinessHoursWeek(restaurantId);

	Map<Long, List<DomainImageItem>> restaurantImages = fileService.getDomainImageUrls(
		DomainType.RESTAURANT,
		List.of(restaurantId));
	RestaurantImageDto image = convertFirstImage(restaurantImages.get(restaurantId));

	Optional<AiRestaurantReviewAnalysis> aiAnalysis = aiRestaurantReviewAnalysisRepository
		.findByRestaurantId(restaurantId);
	String aiSummary = aiAnalysis.map(AiRestaurantReviewAnalysis::getSummary).orElse(null);

	String aiFeature = aiRestaurantFeatureRepository.findByRestaurantId(restaurantId)
		.map(AiRestaurantFeature::getContent)
		.orElse(null);

	// ... 추천 통계 계산 및 응답 조립
}
```

리팩토링 실행 계획: 조회/이미지/AI/스케줄/통계 조립이 한 메서드에 혼재되어 있으므로, `RestaurantReadService`(조회 전용), `RestaurantImageService`, `RestaurantAiSummaryService`, `RestaurantScheduleService`로 책임을 분리하고 응답 조립은 `RestaurantDetailAssembler`로 옮긴다.

### 2. SubgroupService 비대화

```[코드]
@Transactional(readOnly = true)
public CursorPageResponse<SubgroupListItem> searchGroupSubgroups(Long groupId, String keyword, String cursor,
	Integer size) {
	getGroup(groupId);

	int resolvedSize = resolveSize(size);
	SubgroupMemberCountCursor cursorKey = cursorCodec.decodeOrNull(cursor, SubgroupMemberCountCursor.class);
	String resolvedKeyword = resolveKeyword(keyword);

	List<SubgroupListItem> items = subgroupRepository.searchSubgroupsByGroup(
		groupId,
		SubgroupStatus.ACTIVE,
		resolvedKeyword,
		cursorKey == null ? null : cursorKey.memberCount(),
		cursorKey == null ? null : cursorKey.id(),
		PageRequest.of(0, resolvedSize + 1));

	return buildMemberCountCursorPageResponse(applyResolvedImageUrls(items), resolvedSize);
}
```

리팩토링 실행 계획: 검색/커서 처리/이미지 처리 책임을 분리하기 위해 `SubgroupQueryService`(검색/조회)와 `SubgroupCursorPageBuilder`를 도입하고, 이미지 URL 변환은 공통 `DomainImageResolver`로 이동한다.

### 3. GroupService 비대화

```[코드]
@Transactional
public GroupCreateResponse createGroup(GroupCreateRequest request) {
	if (groupRepository.existsByNameAndDeletedAtIsNull(request.name())) {
		throw new BusinessException(GroupErrorCode.ALREADY_EXISTS);
	}

	Group group = Group.builder()
		.name(request.name())
		.type(request.type())
		.address(request.address())
		.detailAddress(request.detailAddress())
		.location(toPoint(request.location()))
		.joinType(request.joinType())
		.emailDomain(request.emailDomain())
		.status(GroupStatus.ACTIVE)
		.build();

	Group savedGroup = groupRepository.save(group);

	if (request.logoImageFileUuid() != null) {
		saveDomainImage(DomainType.GROUP, savedGroup.getId(), request.logoImageFileUuid());
	}

	if (savedGroup.getJoinType() == GroupJoinType.PASSWORD) {
		groupAuthCodeRepository.save(GroupAuthCode.builder()
			.groupId(savedGroup.getId())
			.code(request.code())
			.email(null)
			.expiresAt(null)
			.build());
	}
	return GroupCreateResponse.from(savedGroup);
}
```

리팩토링 실행 계획: 생성 로직에 이미지 연결/인증 코드 저장이 결합되어 있으므로, `GroupCreateService`, `GroupAuthService`, `GroupImageService`로 분리하고 오케스트레이션만 `GroupFacade`에 남긴다.

### 4. 이미지 연결 로직 중복

```[코드]
if (request.imageIds() != null && !request.imageIds().isEmpty()) {
	for (int index = 0; index < request.imageIds().size(); index++) {
		UUID fileUuid = request.imageIds().get(index);
		int sortOrder = index;
		Image image = imageRepository.findByFileUuid(fileUuid)
			.orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
		if (image.getStatus() != ImageStatus.PENDING) {
			throw new BusinessException(FileErrorCode.FILE_NOT_ACTIVE);
		}
		image.activate();
		DomainImage domainImage = DomainImage.create(DomainType.REVIEW, review.getId(), image, sortOrder);
		domainImageRepository.save(domainImage);
	}
}
```

리팩토링 실행 계획: 이미지 검증/활성화/정렬/연결은 도메인 공통 규칙이므로 `DomainImageLinker`(service or component)로 통합하고, 각 서비스는 `linkImages(domainType, domainId, fileUuids)`만 호출하도록 단순화한다.

### 5. 테스트용 API 노출

```[코드]
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
public class WebhookTestController implements WebhookTestControllerDocs {
	// ...
}
```

리팩토링 실행 계획: 운영 환경에서 노출 위험이 있으므로 `@Profile("dev|local")` 적용 또는 `test` 모듈로 이동하고, 문서화/라우팅도 프로필 별로 분리한다.

### 6. FileService 단일 책임 위반

```[코드]
public PresignedUploadResponse createPresignedUploads(PresignedUploadRequest request) {
	List<PresignedUploadItem> uploads = new ArrayList<>();

	for (PresignedUploadFileRequest fileRequest : request.files()) {
		validateUploadPolicy(fileRequest);
		UUID fileUuid = UUID.randomUUID();
		String storageKey = buildStorageKey(request.purpose(), fileUuid, fileRequest.fileName(),
			fileRequest.contentType());
		Image image = Image.create(
			request.purpose(),
			fileRequest.fileName(),
			fileRequest.size(),
			fileRequest.contentType(),
			storageKey,
			fileUuid);
		imageRepository.save(image);
		// ... presigned 생성
	}

	return new PresignedUploadResponse(uploads);
}
```

리팩토링 실행 계획: `FileUploadService`(presigned/정책), `ImageLinkService`(도메인 연결), `ImageQueryService`(조회)로 분리하고 정책 검증은 `FileUploadPolicyValidator`로 추출한다.

### 7. ReviewService 비대화

```[코드]
@Component
@RequiredArgsConstructor
public class ReviewService {
	// 리뷰/키워드/이미지/커서 로직 혼재
}
```

리팩토링 실행 계획: 표준 `@Service`로 변경하고 `ReviewCommandService`(작성/삭제), `ReviewQueryService`(조회/목록), `ReviewKeywordService`(키워드)로 분리한다.

### 8. MainService 조립 로직 중복

```[코드]
List<MainRestaurantDistanceProjection> hotRestaurants = fetchHotSection(location);
List<MainRestaurantDistanceProjection> newRestaurants = fetchNewSection(location);
List<MainRestaurantDistanceProjection> aiRestaurants = fetchAiRecommendSection(location);

Map<Long, String> categoryByRestaurant = fetchCategories(allIds);
Map<Long, String> thumbnailByRestaurant = fetchThumbnails(allIds);
Map<Long, String> summaryByRestaurant = fetchSummaries(allIds);
```

리팩토링 실행 계획: 섹션 조회와 조립을 `MainSectionBuilder`로 분리해 중복을 제거하고, `SectionQueryService`로 데이터 로딩만 담당하게 한다.

### 9. S3StorageClient 복잡도

```[코드]
String policyBase64 = createPolicy(request, expiry, credential, amzDate, sessionToken);
byte[] signatureKey = getSignatureKey(credentials.getAWSSecretKey(), dateStamp, region, "s3");
String signature = bytesToHex(hmacSha256(signatureKey, policyBase64));

Map<String, String> fields = new LinkedHashMap<>();
fields.put("key", request.objectKey());
fields.put("policy", policyBase64);
fields.put("x-amz-algorithm", "AWS4-HMAC-SHA256");
fields.put("x-amz-credential", credential);
```

리팩토링 실행 계획: SigV4 서명/정책 생성은 `S3PresignPolicyBuilder`로 분리하고, `S3StorageClient`는 조립/예외 변환만 담당하도록 축소한다.

### 10. 커서 페이징 패턴 중복

```[코드]
int resolvedSize = resolveSize(size);
SubgroupNameCursor cursorKey = cursorCodec.decodeOrNull(cursor, SubgroupNameCursor.class);

List<SubgroupListItem> items = subgroupRepository.findSubgroupsByGroup(
	groupId,
	SubgroupStatus.ACTIVE,
	cursorKey == null ? null : cursorKey.name(),
	cursorKey == null ? null : cursorKey.id(),
	PageRequest.of(0, resolvedSize + 1));
```

리팩토링 실행 계획: 커서 디코딩/검증/nextCursor 계산을 `CursorPageBuilder`로 공통화하고, 각 서비스는 `fetch`만 담당하도록 단순화한다.

### 11. RestaurantImage 레거시 엔티티

```[코드]
@Deprecated
@Entity
@Table(name = "restaurant_image")
@Comment("음식점 대표 이미지 - DomainImage 사용으로 대체됨")
public class RestaurantImage extends BaseCreatedAtEntity {
	// ...
}
```

리팩토링 실행 계획: 실제 사용 여부를 확인한 뒤 미사용이면 엔티티/레포지토리 제거하고, 사용 중이면 마이그레이션 계획(데이터 이관/삭제 기준)을 문서화한다.

### 12. Swagger docs 중복 구조

```[코드]
@Tag(name = "Group", description = "그룹 관리 API")
public interface GroupControllerDocs {
	@Operation(summary = "그룹 생성", description = "새 그룹을 생성합니다.")
	SuccessResponse<GroupCreateResponse> createGroup(@Validated GroupCreateRequest request);
}
```

리팩토링 실행 계획: `@Controller`에 직접 애노테이션을 붙이거나 공통 메타 애노테이션을 도입해 인터페이스 분리를 제거하고 문서 수정 비용을 줄인다.
