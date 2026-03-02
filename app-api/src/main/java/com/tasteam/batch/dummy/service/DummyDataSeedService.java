package com.tasteam.batch.dummy.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
	private static final int MAX_MEMBER_COUNT = 2000;
	private static final int MAX_RESTAURANT_COUNT = 2000;
	private static final int MAX_GROUP_COUNT = 200;
	private static final int MAX_SUBGROUP_PER_GROUP = 50;
	private static final int MAX_MEMBER_PER_GROUP = 500;
	private static final int MAX_REVIEW_COUNT = 10000;
	private static final int MAX_CHAT_MESSAGE_PER_ROOM = 200;
	private static final long SLOW_STEP_THRESHOLD_MS = 2_000L;

	private final DummyDataJdbcRepository dummyRepo;

	@Transactional
	public AdminDummySeedResponse seed(AdminDummySeedRequest req) {
		long start = System.currentTimeMillis();

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

		// ── Step 1: Member 삽입 ──────────────────────────────────────────────
		List<String> emails = new ArrayList<>(memberCount);
		List<String> nicknames = new ArrayList<>(memberCount);
		for (int i = 0; i < memberCount; i++) {
			emails.add(UUID.randomUUID() + DUMMY_EMAIL_SUFFIX);
			nicknames.add("더미유저" + (i + 1));
		}
		List<Long> memberIds = executeStep("member insert", () -> dummyRepo.insertMembers(emails, nicknames));
		validateStepResult("member insert", memberIds, memberCount);

		// ── Step 2: Restaurant 삽입 ──────────────────────────────────────────
		List<String> restaurantNames = new ArrayList<>(restaurantCount);
		List<String> restaurantAddresses = new ArrayList<>(restaurantCount);
		for (int i = 0; i < restaurantCount; i++) {
			restaurantNames.add("더미식당-" + (i + 1));
			restaurantAddresses.add("서울시 강남구 더미로 " + (i + 1) + "번길 1");
		}
		List<Long> restaurantIds = executeStep("restaurant insert",
			() -> dummyRepo.insertRestaurants(restaurantNames, restaurantAddresses));
		validateStepResult("restaurant insert", restaurantIds, restaurantCount);

		// ── Step 3: Group 삽입 ───────────────────────────────────────────────
		List<String> groupNames = new ArrayList<>(groupCount);
		List<String> groupAddresses = new ArrayList<>(groupCount);
		for (int i = 0; i < groupCount; i++) {
			groupNames.add("더미그룹-" + (i + 1));
			groupAddresses.add("서울시 강남구 테스트로 " + (i + 1));
		}
		List<Long> groupIds = executeStep("group insert", () -> dummyRepo.insertGroups(groupNames, groupAddresses));
		validateStepResult("group insert", groupIds, groupCount);

		// ── Step 4: GroupMember 삽입 ─────────────────────────────────────────
		Map<Long, List<Long>> groupToMembers = new HashMap<>();
		List<long[]> groupMemberPairs = new ArrayList<>();

		for (Long groupId : groupIds) {
			List<Long> shuffled = new ArrayList<>(memberIds);
			Collections.shuffle(shuffled, RANDOM);
			int count = Math.min(memberPerGroup, shuffled.size());
			List<Long> selected = new ArrayList<>(shuffled.subList(0, count));
			groupToMembers.put(groupId, selected);

			Set<String> seen = new HashSet<>();
			for (Long memberId : selected) {
				if (seen.add(groupId + ":" + memberId)) {
					groupMemberPairs.add(new long[] {groupId, memberId});
				}
			}
		}
		executeStep("group_member insert", () -> {
			dummyRepo.insertGroupMembers(groupMemberPairs);
			return null;
		});

		// ── Step 5: Subgroup 삽입 ────────────────────────────────────────────
		List<Long> subgroupGroupIds = new ArrayList<>();
		List<String> subgroupNames = new ArrayList<>();
		for (int g = 0; g < groupIds.size(); g++) {
			for (int s = 0; s < subgroupPerGroup; s++) {
				subgroupGroupIds.add(groupIds.get(g));
				subgroupNames.add("더미팀-" + (g + 1) + "-" + (s + 1));
			}
		}
		List<Long> subgroupIds = executeStep("subgroup insert",
			() -> dummyRepo.insertSubgroups(subgroupGroupIds, subgroupNames));
		validateStepResult("subgroup insert", subgroupIds, subgroupGroupIds.size());

		// subgroupId → groupId 역방향 맵
		Map<Long, Long> subgroupToGroup = new HashMap<>();
		for (int i = 0; i < subgroupIds.size(); i++) {
			subgroupToGroup.put(subgroupIds.get(i), subgroupGroupIds.get(i));
		}

		Map<Long, Long> subgroupToChatRoom = new HashMap<>();
		// ── Step 6: ChatRoom 삽입 (subgroup 1:1) ────────────────────────────
		List<Long> chatRoomIds = executeStep("chat_room insert", () -> dummyRepo.insertChatRooms(subgroupIds));
		validateStepResult("chat_room insert", chatRoomIds, subgroupIds.size());
		for (int i = 0; i < subgroupIds.size(); i++) {
			if (chatRoomIds.get(i) == null) {
				throw new BusinessException(CommonErrorCode.INVALID_DOMAIN_STATE);
			}
			subgroupToChatRoom.put(subgroupIds.get(i), chatRoomIds.get(i));
		}

		// ── Step 7: SubgroupMember + ChatRoomMember 삽입 ────────────────────
		Map<Long, List<Long>> subgroupToMembers = new HashMap<>();
		List<long[]> subgroupMemberPairs = new ArrayList<>();
		List<long[]> chatRoomMemberPairs = new ArrayList<>();
		Map<Long, Integer> subgroupMemberCounts = new HashMap<>();

		for (Long subgroupId : subgroupIds) {
			Long groupId = subgroupToGroup.get(subgroupId);
			List<Long> groupMembers = groupToMembers.get(groupId);
			if (groupMembers == null || groupMembers.isEmpty()) {
				continue;
			}

			List<Long> shuffled = new ArrayList<>(groupMembers);
			Collections.shuffle(shuffled, RANDOM);
			int count = Math.min(memberPerGroup, shuffled.size());
			List<Long> selected = new ArrayList<>(shuffled.subList(0, count));

			subgroupToMembers.put(subgroupId, selected);
			subgroupMemberCounts.put(subgroupId, selected.size());

			Long chatRoomId = subgroupToChatRoom.get(subgroupId);
			if (chatRoomId == null) {
				throw new BusinessException(CommonErrorCode.INVALID_DOMAIN_STATE);
			}
			Set<String> sgSeen = new HashSet<>();
			Set<String> crSeen = new HashSet<>();

			for (Long memberId : selected) {
				if (sgSeen.add(subgroupId + ":" + memberId)) {
					subgroupMemberPairs.add(new long[] {subgroupId, memberId});
				}
				if (crSeen.add(chatRoomId + ":" + memberId)) {
					chatRoomMemberPairs.add(new long[] {chatRoomId, memberId});
				}
			}
		}
		executeStep("subgroup_member insert", () -> {
			dummyRepo.insertSubgroupMembers(subgroupMemberPairs);
			return null;
		});
		if (!subgroupMemberCounts.isEmpty()) {
			executeStep("subgroup member count update", () -> {
				dummyRepo.updateSubgroupMemberCounts(subgroupMemberCounts);
				return null;
			});
		}
		executeStep("chat_room_member insert", () -> {
			dummyRepo.insertChatRoomMembers(chatRoomMemberPairs);
			return null;
		});

		// ── Step 8: Review 삽입 ──────────────────────────────────────────────
		int actualMemberCount = Math.max(1, memberIds.size());
		int actualRestaurantCount = Math.max(1, restaurantIds.size());
		int actualGroupCount = Math.max(1, groupIds.size());

		List<long[]> reviewEntries = new ArrayList<>(reviewCount);
		List<String> reviewContents = new ArrayList<>(reviewCount);
		for (int r = 0; r < reviewCount; r++) {
			Long memberId = memberIds.get(r % actualMemberCount);
			Long restaurantId = restaurantIds.get(r % actualRestaurantCount);
			Long groupId = groupIds.get(r % actualGroupCount);
			int recommended = RANDOM.nextBoolean() ? 1 : 0;
			reviewEntries.add(new long[] {memberId, restaurantId, groupId, recommended});
			reviewContents.add("더미 리뷰 내용입니다. " + (r + 1));
		}
		List<Long> reviewIds = executeStep("review insert",
			() -> dummyRepo.insertReviews(reviewEntries, reviewContents));
		validateStepResult("review insert", reviewIds, reviewCount);

		// ── Step 9: ReviewKeyword 삽입 ───────────────────────────────────────
		List<Long> keywordIds = dummyRepo.queryKeywordIds();
		if (!keywordIds.isEmpty() && !reviewIds.isEmpty()) {
			List<long[]> reviewKeywordPairs = new ArrayList<>();
			Set<String> rkSeen = new HashSet<>();
			for (Long reviewId : reviewIds) {
				int numKeywords = 1 + RANDOM.nextInt(3);
				List<Long> shuffledKw = new ArrayList<>(keywordIds);
				Collections.shuffle(shuffledKw, RANDOM);
				for (int k = 0; k < Math.min(numKeywords, shuffledKw.size()); k++) {
					String key = reviewId + ":" + shuffledKw.get(k);
					if (rkSeen.add(key)) {
						reviewKeywordPairs.add(new long[] {reviewId, shuffledKw.get(k)});
					}
				}
			}
			executeStep("review_keyword insert", () -> {
				dummyRepo.insertReviewKeywords(reviewKeywordPairs);
				return null;
			});
		}

		// ── Step 10: ChatMessage 삽입 ────────────────────────────────────────
		List<long[]> chatMessageEntries = new ArrayList<>();
		List<String> chatMessageContents = new ArrayList<>();
		int msgCounter = 0;
		for (Long subgroupId : subgroupIds) {
			Long chatRoomId = subgroupToChatRoom.get(subgroupId);
			List<Long> members = subgroupToMembers.get(subgroupId);
			if (members == null || members.isEmpty()) {
				continue;
			}
			for (int m = 0; m < chatMessagePerRoom; m++) {
				Long memberId = members.get(m % members.size());
				chatMessageEntries.add(new long[] {chatRoomId, memberId});
				chatMessageContents.add("더미 채팅 메시지 " + (++msgCounter));
			}
		}
		int chatMessagesInserted = executeStep("chat_message insert",
			() -> dummyRepo.insertChatMessages(chatMessageEntries, chatMessageContents));
		if (chatMessageEntries.size() != chatMessagesInserted) {
			log.warn("[DummySeed] chat_message insert count mismatch. expected={} actual={}",
				chatMessageEntries.size(),
				chatMessagesInserted);
		}

		long elapsed = System.currentTimeMillis() - start;
		log.info("[DummySeed] completed in {}ms", elapsed);
		return new AdminDummySeedResponse(
			memberIds.size(),
			restaurantIds.size(),
			groupIds.size(),
			subgroupIds.size(),
			reviewIds.size(),
			chatMessagesInserted,
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
			throw e;
		} catch (Exception e) {
			log.error("[DummySeed] {} failed", stepName, e);
			throw e;
		}
	}
}
