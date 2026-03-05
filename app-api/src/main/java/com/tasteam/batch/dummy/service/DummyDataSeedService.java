package com.tasteam.batch.dummy.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.tasteam.batch.dummy.repository.DummyDataJdbcRepository;
import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.domain.admin.dto.response.AdminDataCountResponse;
import com.tasteam.domain.admin.dto.response.AdminDummySeedResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DummyDataSeedService {

	private static final String DUMMY_EMAIL_SUFFIX = "@dummy.tasteam.kr";
	private static final Random RANDOM = new Random();
	private static final int DEFAULT_MEMBER_COUNT = 500;
	private static final int DEFAULT_RESTAURANT_COUNT = 200;
	private static final int DEFAULT_GROUP_COUNT = 20;
	private static final int DEFAULT_SUBGROUP_PER_GROUP = 5;
	private static final int DEFAULT_MEMBER_PER_GROUP = 30;
	private static final int DEFAULT_REVIEW_COUNT = 1000;
	private static final int DEFAULT_CHAT_MESSAGE_PER_ROOM = 50;
	private static final int MAX_MEMBER_COUNT = 100_000;
	private static final int MAX_RESTAURANT_COUNT = 50_000_000;
	private static final int MAX_GROUP_COUNT = 1_000;
	private static final int MAX_SUBGROUP_PER_GROUP = 1_000;
	private static final int MAX_MEMBER_PER_GROUP = 1_000;
	private static final int MAX_REVIEW_COUNT = 100_000_000;
	private static final int MAX_CHAT_MESSAGE_PER_ROOM = 1_000_000_000;
	private static final int SERVICE_CHUNK_SIZE = 5_000;
	private static final int RELATION_FLUSH_SIZE = 5_000;
	private static final int MAX_KEYWORDS_PER_REVIEW = 3;
	private static final long SLOW_STEP_THRESHOLD_MS = 2_000L;

	private final DummyDataJdbcRepository dummyRepo;

	public AdminDummySeedResponse seed(AdminDummySeedRequest req) {
		long start = System.currentTimeMillis();
		String runToken = UUID.randomUUID().toString();

		int memberCount = normalizeCount("memberCount", req.memberCount(), DEFAULT_MEMBER_COUNT, MAX_MEMBER_COUNT);
		int restaurantCount = normalizeCount("restaurantCount", req.restaurantCount(), DEFAULT_RESTAURANT_COUNT,
			MAX_RESTAURANT_COUNT);
		int groupCount = normalizeCount("groupCount", req.groupCount(), DEFAULT_GROUP_COUNT, MAX_GROUP_COUNT);
		int subgroupPerGroup = normalizeCount("subgroupPerGroup", req.subgroupPerGroup(), DEFAULT_SUBGROUP_PER_GROUP,
			MAX_SUBGROUP_PER_GROUP);
		int memberPerGroup = normalizeCount("memberPerGroup", req.memberPerGroup(), DEFAULT_MEMBER_PER_GROUP,
			MAX_MEMBER_PER_GROUP);
		int reviewCount = normalizeCount("reviewCount", req.reviewCount(), DEFAULT_REVIEW_COUNT, MAX_REVIEW_COUNT);
		int chatMessagePerRoom = normalizeCount("chatMessagePerRoom", req.chatMessagePerRoom(),
			DEFAULT_CHAT_MESSAGE_PER_ROOM, MAX_CHAT_MESSAGE_PER_ROOM);

		// 순차 실행: 병렬 대량 INSERT는 DB WAL/connection pool 경합을 유발하므로 직렬화
		LongIdBuffer memberIds = executeStep("member insert", () -> insertMembers(memberCount, runToken));
		LongIdBuffer restaurantIds = executeStep("restaurant insert",
			() -> insertRestaurants(restaurantCount, runToken));
		LongIdBuffer groupIds = executeStep("group insert", () -> insertGroups(groupCount, runToken));

		GroupSeedResult groupSeedResult = executeStep(
			"group_member/subgroup/chat relation insert",
			() -> insertGroupRelatedData(groupIds, memberIds, subgroupPerGroup, memberPerGroup, chatMessagePerRoom));

		int reviewsInserted = executeStep("review/review_keyword insert",
			() -> insertReviewsWithKeywords(reviewCount, memberIds, restaurantIds, groupIds));

		long elapsed = System.currentTimeMillis() - start;
		log.info("[DummySeed] completed in {}ms", elapsed);
		return new AdminDummySeedResponse(
			memberIds.size(),
			restaurantIds.size(),
			groupIds.size(),
			groupSeedResult.subgroupsInserted(),
			reviewsInserted,
			groupSeedResult.chatMessagesInserted(),
			elapsed);
	}

	@Transactional(readOnly = true)
	public AdminDataCountResponse count() {
		return new AdminDataCountResponse(
			dummyRepo.countMembers(),
			dummyRepo.countRestaurants(),
			dummyRepo.countGroups(),
			dummyRepo.countSubgroups(),
			dummyRepo.countReviews(),
			dummyRepo.countChatMessages());
	}

	@Transactional
	public void deleteDummyData() {
		dummyRepo.deleteDummyData();
	}

	private LongIdBuffer insertMembers(int memberCount, String runToken) {
		LongIdBuffer ids = new LongIdBuffer(initialCapacity(memberCount));
		for (int start = 0; start < memberCount; start += SERVICE_CHUNK_SIZE) {
			int end = Math.min(start + SERVICE_CHUNK_SIZE, memberCount);
			int size = end - start;
			List<String> emails = new ArrayList<>(size);
			List<String> nicknames = new ArrayList<>(size);

			for (int i = 0; i < size; i++) {
				int seq = start + i + 1;
				emails.add("dummy-" + runToken + "-" + seq + DUMMY_EMAIL_SUFFIX);
				nicknames.add("더미유저-" + runToken + "-" + seq);
			}
			List<Long> chunkIds = dummyRepo.insertMembers(emails, nicknames);
			validateStepResult("member insert", chunkIds, size);
			ids.addAll(chunkIds);
		}
		validateInsertedCount("member insert", ids.size(), memberCount);
		return ids;
	}

	private LongIdBuffer insertRestaurants(int restaurantCount, String runToken) {
		LongIdBuffer ids = new LongIdBuffer(initialCapacity(restaurantCount));
		for (int start = 0; start < restaurantCount; start += SERVICE_CHUNK_SIZE) {
			int end = Math.min(start + SERVICE_CHUNK_SIZE, restaurantCount);
			int size = end - start;
			List<String> restaurantNames = new ArrayList<>(size);
			List<String> restaurantAddresses = new ArrayList<>(size);

			for (int i = 0; i < size; i++) {
				int seq = start + i + 1;
				restaurantNames.add("더미식당-" + runToken + "-" + seq);
				restaurantAddresses.add("서울시 강남구 더미로 " + seq + "번길 1");
			}
			List<Long> chunkIds = dummyRepo.insertRestaurants(restaurantNames, restaurantAddresses);
			validateStepResult("restaurant insert", chunkIds, size);
			ids.addAll(chunkIds);
		}
		validateInsertedCount("restaurant insert", ids.size(), restaurantCount);
		return ids;
	}

	private LongIdBuffer insertGroups(int groupCount, String runToken) {
		LongIdBuffer ids = new LongIdBuffer(initialCapacity(groupCount));
		for (int start = 0; start < groupCount; start += SERVICE_CHUNK_SIZE) {
			int end = Math.min(start + SERVICE_CHUNK_SIZE, groupCount);
			int size = end - start;
			List<String> groupNames = new ArrayList<>(size);
			List<String> groupAddresses = new ArrayList<>(size);

			for (int i = 0; i < size; i++) {
				int seq = start + i + 1;
				groupNames.add("더미그룹-" + runToken + "-" + seq);
				groupAddresses.add("서울시 강남구 테스트로 " + seq);
			}
			List<Long> chunkIds = dummyRepo.insertGroups(groupNames, groupAddresses);
			validateStepResult("group insert", chunkIds, size);
			ids.addAll(chunkIds);
		}
		validateInsertedCount("group insert", ids.size(), groupCount);
		return ids;
	}

	private GroupSeedResult insertGroupRelatedData(
		LongIdBuffer groupIds,
		LongIdBuffer memberIds,
		int subgroupPerGroup,
		int memberPerGroup,
		int chatMessagePerRoom) {

		int totalSubgroups = groupIds.size() * subgroupPerGroup;

		// Phase 1: 모든 그룹의 subgroup을 한 번에 batch INSERT (그룹별 루프 왕복 제거)
		List<Long> allSubgroupGroupIds = new ArrayList<>(totalSubgroups);
		List<String> allSubgroupNames = new ArrayList<>(totalSubgroups);
		for (int groupIndex = 0; groupIndex < groupIds.size(); groupIndex++) {
			long groupId = groupIds.get(groupIndex);
			for (int s = 0; s < subgroupPerGroup; s++) {
				allSubgroupGroupIds.add(groupId);
				allSubgroupNames.add("더미팀-" + (groupIndex + 1) + "-" + (s + 1));
			}
		}

		List<Long> allSubgroupIds = new ArrayList<>(totalSubgroups);
		for (int start = 0; start < totalSubgroups; start += SERVICE_CHUNK_SIZE) {
			int end = Math.min(start + SERVICE_CHUNK_SIZE, totalSubgroups);
			allSubgroupIds.addAll(
				dummyRepo.insertSubgroups(allSubgroupGroupIds.subList(start, end),
					allSubgroupNames.subList(start, end)));
		}
		validateStepResult("subgroup batch insert", allSubgroupIds, totalSubgroups);

		// Phase 2: 모든 subgroup에 대한 chatroom을 한 번에 batch INSERT
		List<Long> allChatRoomIds = new ArrayList<>(totalSubgroups);
		for (int start = 0; start < totalSubgroups; start += SERVICE_CHUNK_SIZE) {
			int end = Math.min(start + SERVICE_CHUNK_SIZE, totalSubgroups);
			allChatRoomIds.addAll(dummyRepo.insertChatRooms(allSubgroupIds.subList(start, end)));
		}
		validateStepResult("chatroom batch insert", allChatRoomIds, totalSubgroups);

		// Phase 3: 관계 데이터는 메모리에 누적 후 flush (DB 왕복 없이 루프)
		long chatMessagesInserted = 0L;
		long messageSerial = 0L;

		List<long[]> groupMemberPairs = new ArrayList<>(RELATION_FLUSH_SIZE);
		List<long[]> subgroupMemberPairs = new ArrayList<>(RELATION_FLUSH_SIZE);
		List<long[]> chatRoomMemberPairs = new ArrayList<>(RELATION_FLUSH_SIZE);
		Map<Long, Integer> subgroupMemberCounts = new HashMap<>(RELATION_FLUSH_SIZE);
		List<long[]> chatMessageEntries = new ArrayList<>(RELATION_FLUSH_SIZE);
		List<String> chatMessageContents = new ArrayList<>(RELATION_FLUSH_SIZE);

		for (int groupIndex = 0; groupIndex < groupIds.size(); groupIndex++) {
			long groupId = groupIds.get(groupIndex);
			long[] selectedGroupMembers = selectCyclicMembers(memberIds, memberPerGroup, groupIndex + 1);

			appendGroupMemberPairs(groupId, selectedGroupMembers, groupMemberPairs);
			if (groupMemberPairs.size() >= RELATION_FLUSH_SIZE) {
				flushGroupMemberPairs(groupMemberPairs);
			}

			int subgroupBase = groupIndex * subgroupPerGroup;
			for (int s = 0; s < subgroupPerGroup; s++) {
				Long subgroupId = allSubgroupIds.get(subgroupBase + s);
				Long chatRoomId = allChatRoomIds.get(subgroupBase + s);
				if (chatRoomId == null) {
					throw new BusinessException(CommonErrorCode.INVALID_DOMAIN_STATE);
				}

				int subgroupMemberCount = selectedGroupMembers.length;
				subgroupMemberCounts.put(subgroupId, subgroupMemberCount);
				int memberOffset = subgroupMemberCount == 0
					? 0
					: Math.floorMod((groupIndex + 1) * 31 + (s + 1) * 17, subgroupMemberCount);

				appendSubgroupAndChatRoomMembers(
					subgroupId,
					chatRoomId,
					selectedGroupMembers,
					memberOffset,
					subgroupMemberPairs,
					chatRoomMemberPairs);

				for (int m = 0; m < chatMessagePerRoom; m++) {
					if (subgroupMemberCount == 0) {
						break;
					}
					long memberId = selectedGroupMembers[(memberOffset + (m % subgroupMemberCount))
						% subgroupMemberCount];
					chatMessageEntries.add(new long[] {chatRoomId, memberId});
					chatMessageContents.add("더미 채팅 메시지 " + (++messageSerial));
					if (chatMessageEntries.size() >= RELATION_FLUSH_SIZE) {
						chatMessagesInserted += flushChatMessages(chatMessageEntries, chatMessageContents);
					}
				}

				if (subgroupMemberPairs.size() >= RELATION_FLUSH_SIZE) {
					flushSubgroupMemberPairs(subgroupMemberPairs);
				}
				if (chatRoomMemberPairs.size() >= RELATION_FLUSH_SIZE) {
					flushChatRoomMemberPairs(chatRoomMemberPairs);
				}
				if (subgroupMemberCounts.size() >= RELATION_FLUSH_SIZE) {
					flushSubgroupMemberCounts(subgroupMemberCounts);
				}
			}
		}

		flushGroupMemberPairs(groupMemberPairs);
		flushSubgroupMemberPairs(subgroupMemberPairs);
		flushChatRoomMemberPairs(chatRoomMemberPairs);
		flushSubgroupMemberCounts(subgroupMemberCounts);
		chatMessagesInserted += flushChatMessages(chatMessageEntries, chatMessageContents);

		return new GroupSeedResult(allSubgroupIds.size(), chatMessagesInserted);
	}

	private int insertReviewsWithKeywords(
		int reviewCount,
		LongIdBuffer memberIds,
		LongIdBuffer restaurantIds,
		LongIdBuffer groupIds) {

		List<Long> keywordIds = dummyRepo.queryKeywordIds();
		int reviewsInserted = 0;

		for (int start = 0; start < reviewCount; start += SERVICE_CHUNK_SIZE) {
			int end = Math.min(start + SERVICE_CHUNK_SIZE, reviewCount);
			int size = end - start;
			List<long[]> reviewEntries = new ArrayList<>(size);
			List<String> reviewContents = new ArrayList<>(size);

			for (int i = 0; i < size; i++) {
				int seq = start + i;
				long memberId = memberIds.get(seq % memberIds.size());
				long restaurantId = restaurantIds.get(seq % restaurantIds.size());
				long groupId = groupIds.get(seq % groupIds.size());
				int recommended = RANDOM.nextBoolean() ? 1 : 0;
				reviewEntries.add(new long[] {memberId, restaurantId, groupId, recommended});
				reviewContents.add("더미 리뷰 내용입니다. " + (seq + 1));
			}

			List<Long> reviewIds = dummyRepo.insertReviews(reviewEntries, reviewContents);
			validateStepResult("review insert", reviewIds, size);
			reviewsInserted += reviewIds.size();

			if (!keywordIds.isEmpty()) {
				List<long[]> reviewKeywordPairs = new ArrayList<>(reviewIds.size() * 2);
				int keywordSize = keywordIds.size();
				int maxPerReview = Math.min(MAX_KEYWORDS_PER_REVIEW, keywordSize);

				for (Long reviewId : reviewIds) {
					int keywordCount = 1 + RANDOM.nextInt(maxPerReview);
					int keywordStartIndex = Math.floorMod((int)(reviewId % keywordSize), keywordSize);
					for (int k = 0; k < keywordCount; k++) {
						long keywordId = keywordIds.get((keywordStartIndex + k) % keywordSize);
						reviewKeywordPairs.add(new long[] {reviewId, keywordId});
					}
				}

				if (!reviewKeywordPairs.isEmpty()) {
					dummyRepo.insertReviewKeywords(reviewKeywordPairs);
				}
			}
		}

		validateInsertedCount("review insert", reviewsInserted, reviewCount);
		return reviewsInserted;
	}

	private long[] selectCyclicMembers(LongIdBuffer pool, int requestedCount, int seed) {
		int poolSize = pool.size();
		int count = Math.min(requestedCount, poolSize);
		long[] selected = new long[count];
		if (count == 0) {
			return selected;
		}

		int start = Math.floorMod(seed * 37, poolSize);
		for (int i = 0; i < count; i++) {
			selected[i] = pool.get((start + i) % poolSize);
		}
		return selected;
	}

	private void appendGroupMemberPairs(long groupId, long[] members, List<long[]> pairs) {
		for (long memberId : members) {
			pairs.add(new long[] {groupId, memberId});
		}
	}

	private void appendSubgroupAndChatRoomMembers(
		long subgroupId,
		long chatRoomId,
		long[] members,
		int offset,
		List<long[]> subgroupMemberPairs,
		List<long[]> chatRoomMemberPairs) {

		int count = members.length;
		for (int i = 0; i < count; i++) {
			long memberId = members[(offset + i) % count];
			subgroupMemberPairs.add(new long[] {subgroupId, memberId});
			chatRoomMemberPairs.add(new long[] {chatRoomId, memberId});
		}
	}

	private void flushGroupMemberPairs(List<long[]> pairs) {
		if (pairs.isEmpty()) {
			return;
		}
		dummyRepo.insertGroupMembers(pairs);
		pairs.clear();
	}

	private void flushSubgroupMemberPairs(List<long[]> pairs) {
		if (pairs.isEmpty()) {
			return;
		}
		dummyRepo.insertSubgroupMembers(pairs);
		pairs.clear();
	}

	private void flushChatRoomMemberPairs(List<long[]> pairs) {
		if (pairs.isEmpty()) {
			return;
		}
		dummyRepo.insertChatRoomMembers(pairs);
		pairs.clear();
	}

	private void flushSubgroupMemberCounts(Map<Long, Integer> counts) {
		if (counts.isEmpty()) {
			return;
		}
		dummyRepo.updateSubgroupMemberCounts(counts);
		counts.clear();
	}

	private int flushChatMessages(List<long[]> entries, List<String> contents) {
		if (entries.isEmpty()) {
			return 0;
		}
		int inserted = dummyRepo.insertChatMessages(entries, contents);
		entries.clear();
		contents.clear();
		return inserted;
	}

	private int normalizeCount(String fieldName, int value, int defaultValue, int maxValue) {
		if (value <= 0) {
			return defaultValue;
		}
		if (value > maxValue) {
			log.warn("[DummySeed] request value too large. field={} value={} max={}", fieldName, value, maxValue);
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST);
		}
		return value;
	}

	private int initialCapacity(int expectedCount) {
		return Math.min(expectedCount, SERVICE_CHUNK_SIZE);
	}

	private void validateInsertedCount(String stepName, int actual, int expected) {
		if (actual != expected) {
			log.error("[DummySeed] {} expected {} but got {}", stepName, expected, actual);
			throw new BusinessException(CommonErrorCode.INVALID_DOMAIN_STATE);
		}
	}

	private void validateStepResult(String stepName, List<?> values, int expectedSize) {
		if (CollectionUtils.isEmpty(values)) {
			throw new BusinessException(CommonErrorCode.INVALID_DOMAIN_STATE);
		}
		if (expectedSize > 0 && values.size() != expectedSize) {
			log.error("[DummySeed] {} expected {} but got {}", stepName, expectedSize, values.size());
			throw new BusinessException(CommonErrorCode.INVALID_DOMAIN_STATE);
		}
	}

	private <T> T executeStep(String stepName, Supplier<T> step) {
		long start = System.currentTimeMillis();
		try {
			T result = step.get();
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed >= SLOW_STEP_THRESHOLD_MS) {
				log.warn("[DummySeed] {} is slow: {}ms", stepName, elapsed);
			} else {
				log.info("[DummySeed] {} completed in {}ms", stepName, elapsed);
			}
			return result;
		} catch (BusinessException e) {
			log.error("[DummySeed] {} failed: {}", stepName, e.getErrorCode(), e);
			throw new DummySeedStepException(stepName, e.getErrorMessage(), e);
		} catch (Exception e) {
			log.error("[DummySeed] {} failed", stepName, e);
			throw new DummySeedStepException(stepName, e.getMessage(), e);
		}
	}

	private record GroupSeedResult(long subgroupsInserted, long chatMessagesInserted) {
	}

	public static final class DummySeedStepException extends RuntimeException {

		private final String stepName;

		public DummySeedStepException(String stepName, String cause, Throwable original) {
			super("[" + stepName + "] 단계 실패: " + cause, original);
			this.stepName = stepName;
		}

		public String getStepName() {
			return stepName;
		}
	}

	private static final class LongIdBuffer {

		private long[] values;
		private int size;

		private LongIdBuffer(int initialCapacity) {
			this.values = new long[Math.max(16, initialCapacity)];
		}

		private void addAll(List<Long> ids) {
			ensureCapacity(size + ids.size());
			for (Long id : ids) {
				values[size++] = id;
			}
		}

		private int size() {
			return size;
		}

		private long get(int index) {
			if (index < 0 || index >= size) {
				throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
			}
			return values[index];
		}

		private void ensureCapacity(int required) {
			if (required <= values.length) {
				return;
			}
			int newCapacity = values.length;
			while (newCapacity < required) {
				newCapacity = newCapacity << 1;
			}
			values = Arrays.copyOf(values, newCapacity);
		}
	}
}
