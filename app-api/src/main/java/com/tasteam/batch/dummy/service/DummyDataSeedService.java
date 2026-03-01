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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.batch.dummy.repository.DummyDataJdbcRepository;
import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.domain.admin.dto.response.AdminDataCountResponse;
import com.tasteam.domain.admin.dto.response.AdminDummySeedResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DummyDataSeedService {

	private static final String DUMMY_EMAIL_SUFFIX = "@dummy.tasteam.kr";
	private static final Random RANDOM = new Random();

	private final DummyDataJdbcRepository dummyRepo;

	@Transactional
	public AdminDummySeedResponse seed(AdminDummySeedRequest req) {
		long start = System.currentTimeMillis();

		int memberCount = req.memberCount() > 0 ? req.memberCount() : 500;
		int restaurantCount = req.restaurantCount() > 0 ? req.restaurantCount() : 200;
		int groupCount = req.groupCount() > 0 ? req.groupCount() : 20;
		int subgroupPerGroup = req.subgroupPerGroup() > 0 ? req.subgroupPerGroup() : 5;
		int memberPerGroup = req.memberPerGroup() > 0 ? req.memberPerGroup() : 30;
		int reviewCount = req.reviewCount() > 0 ? req.reviewCount() : 1000;
		int chatMessagePerRoom = req.chatMessagePerRoom() > 0 ? req.chatMessagePerRoom() : 50;

		// ── Step 1: Member 삽입 ──────────────────────────────────────────────
		List<String> emails = new ArrayList<>(memberCount);
		List<String> nicknames = new ArrayList<>(memberCount);
		for (int i = 0; i < memberCount; i++) {
			emails.add(UUID.randomUUID() + DUMMY_EMAIL_SUFFIX);
			nicknames.add("더미유저" + (i + 1));
		}
		List<Long> memberIds = dummyRepo.insertMembers(emails, nicknames);

		// ── Step 2: Restaurant 삽입 ──────────────────────────────────────────
		List<String> restaurantNames = new ArrayList<>(restaurantCount);
		List<String> restaurantAddresses = new ArrayList<>(restaurantCount);
		for (int i = 0; i < restaurantCount; i++) {
			restaurantNames.add("더미식당-" + (i + 1));
			restaurantAddresses.add("서울시 강남구 더미로 " + (i + 1) + "번길 1");
		}
		List<Long> restaurantIds = dummyRepo.insertRestaurants(restaurantNames, restaurantAddresses);

		// ── Step 3: Group 삽입 ───────────────────────────────────────────────
		List<String> groupNames = new ArrayList<>(groupCount);
		List<String> groupAddresses = new ArrayList<>(groupCount);
		for (int i = 0; i < groupCount; i++) {
			groupNames.add("더미그룹-" + (i + 1));
			groupAddresses.add("서울시 강남구 테스트로 " + (i + 1));
		}
		List<Long> groupIds = dummyRepo.insertGroups(groupNames, groupAddresses);

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
		dummyRepo.insertGroupMembers(groupMemberPairs);

		// ── Step 5: Subgroup 삽입 ────────────────────────────────────────────
		List<Long> subgroupGroupIds = new ArrayList<>();
		List<String> subgroupNames = new ArrayList<>();
		for (int g = 0; g < groupIds.size(); g++) {
			for (int s = 0; s < subgroupPerGroup; s++) {
				subgroupGroupIds.add(groupIds.get(g));
				subgroupNames.add("더미팀-" + (g + 1) + "-" + (s + 1));
			}
		}
		List<Long> subgroupIds = dummyRepo.insertSubgroups(subgroupGroupIds, subgroupNames);

		// subgroupId → groupId 역방향 맵
		Map<Long, Long> subgroupToGroup = new HashMap<>();
		for (int i = 0; i < subgroupIds.size(); i++) {
			subgroupToGroup.put(subgroupIds.get(i), subgroupGroupIds.get(i));
		}

		// ── Step 6: ChatRoom 삽입 (subgroup 1:1) ────────────────────────────
		List<Long> chatRoomIds = dummyRepo.insertChatRooms(subgroupIds);

		Map<Long, Long> subgroupToChatRoom = new HashMap<>();
		for (int i = 0; i < subgroupIds.size(); i++) {
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
		dummyRepo.insertSubgroupMembers(subgroupMemberPairs);
		if (!subgroupMemberCounts.isEmpty()) {
			dummyRepo.updateSubgroupMemberCounts(subgroupMemberCounts);
		}
		dummyRepo.insertChatRoomMembers(chatRoomMemberPairs);

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
		List<Long> reviewIds = dummyRepo.insertReviews(reviewEntries, reviewContents);

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
			dummyRepo.insertReviewKeywords(reviewKeywordPairs);
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
		int chatMessagesInserted = dummyRepo.insertChatMessages(chatMessageEntries, chatMessageContents);

		long elapsed = System.currentTimeMillis() - start;
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
}
