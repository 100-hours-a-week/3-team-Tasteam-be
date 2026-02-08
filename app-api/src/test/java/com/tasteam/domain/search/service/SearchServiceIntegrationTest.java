package com.tasteam.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.group.entity.Group;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.domain.search.repository.MemberSearchHistoryRepository;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.SearchErrorCode;

@ServiceIntegrationTest
@Transactional
class SearchServiceIntegrationTest {

	@Autowired
	private SearchService searchService;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberSearchHistoryRepository searchHistoryRepository;

	@Nested
	@DisplayName("통합 검색")
	class Search {

		@Test
		@Disabled
		@DisplayName("그룹+음식점 검색 결과가 반환되고 검색 히스토리가 기록된다")
		void searchSuccessRecordsHistory() {
			Member member = memberRepository.save(MemberFixture.create("search@example.com", "search"));
			Group group = groupRepository.save(GroupFixture.create("맛집 모임", "서울특별시 강남구"));
			restaurantRepository.save(createRestaurant("맛집 식당"));

			SearchResponse response = searchService.search(
				member.getId(),
				new SearchRequest("맛집", null, null, null, 10));

			assertThat(response.groups()).hasSize(1);
			assertThat(response.restaurants().items()).hasSize(1);

			assertThat(searchHistoryRepository.findByMemberIdAndKeywordAndDeletedAtIsNull(
				member.getId(), "맛집")).isPresent();
		}

		@Test
		@DisplayName("잘못된 커서이면 빈 결과로 처리된다")
		void searchInvalidCursorReturnsEmpty() {
			SearchResponse response = searchService.search(
				null,
				new SearchRequest("맛집", null, null, "invalid-cursor", 10));

			assertThat(response.groups()).isEmpty();
			assertThat(response.restaurants().items()).isEmpty();
		}
	}

	@Nested
	@DisplayName("최근 검색어 조회")
	class GetRecentSearches {

		@Test
		@DisplayName("인증되지 않은 사용자면 실패한다")
		void getRecentSearchesUnauthorizedFails() {
			assertThatThrownBy(() -> searchService.getRecentSearches(null))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(CommonErrorCode.AUTHENTICATION_REQUIRED.name());
		}
	}

	@Nested
	@DisplayName("최근 검색어 삭제")
	class DeleteRecentSearch {

		@Test
		@DisplayName("존재하지 않는 기록이면 실패한다")
		void deleteRecentSearchNotFoundFails() {
			Member member = memberRepository.save(MemberFixture.create("delete@example.com", "delete"));

			assertThatThrownBy(() -> searchService.deleteRecentSearch(member.getId(), 999999L))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(SearchErrorCode.RECENT_SEARCH_NOT_FOUND.name());
		}
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울특별시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-7777-8888");
	}
}
