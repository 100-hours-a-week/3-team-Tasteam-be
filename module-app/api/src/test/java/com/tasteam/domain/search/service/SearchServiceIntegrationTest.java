package com.tasteam.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.group.repository.GroupRepository;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.search.dto.request.SearchRequest;
import com.tasteam.domain.search.dto.response.SearchResponse;
import com.tasteam.fixture.GroupFixture;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.SearchErrorCode;

@ServiceIntegrationTest
@Transactional
@DisplayName("[통합](Search) SearchService 통합 테스트")
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
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void createSearchMvIfAbsent() {
		jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
		jdbcTemplate.execute("""
			CREATE MATERIALIZED VIEW IF NOT EXISTS restaurant_search_mv AS
			SELECT
			    r.id             AS restaurant_id,
			    r.name           AS name,
			    r.full_address   AS full_address,
			    lower(r.name)         AS name_lower,
			    lower(r.full_address) AS addr_lower,
			    r.location,
			    r.updated_at,
			    r.deleted_at,
			    COALESCE(
			        array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL),
			        ARRAY[]::text[]
			    ) AS category_names,
			    setweight(to_tsvector('simple', coalesce(lower(r.name), '')), 'A')
			    || setweight(to_tsvector('simple', coalesce(
			        array_to_string(
			            COALESCE(
			                array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL),
			                ARRAY[]::text[]
			            ), ' '), '')), 'B')
			    || setweight(to_tsvector('simple', coalesce(lower(r.full_address), '')), 'C')
			    AS search_vector
			FROM restaurant r
			LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
			LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
			GROUP BY r.id, r.name, r.full_address, r.location, r.updated_at, r.deleted_at
			""");
	}

	@Nested
	@DisplayName("통합 검색")
	class Search {

		@Test
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		@DisplayName("그룹+음식점 검색 결과가 반환된다")
		void searchSuccessReturnsResults() {
			Member member = memberRepository.save(MemberFixture.create("search@example.com", "search"));
			groupRepository.save(GroupFixture.create("맛집 모임", "서울특별시 강남구"));
			restaurantRepository.save(createRestaurant("맛집 식당"));

			jdbcTemplate.execute("REFRESH MATERIALIZED VIEW restaurant_search_mv");

			SearchResponse response = searchService.search(
				member.getId(),
				new SearchRequest("맛집", null, null, null, null, 10));

			assertThat(response.groups()).hasSize(1);
			assertThat(response.restaurants().items()).hasSize(1);
		}

		@AfterEach
		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		void cleanUp() {
			jdbcTemplate.execute("DELETE FROM restaurant WHERE name = '맛집 식당'");
			jdbcTemplate.execute("DELETE FROM \"group\" WHERE name = '맛집 모임'");
			jdbcTemplate.execute("DELETE FROM member WHERE email = 'search@example.com'");
		}

		@Test
		@DisplayName("잘못된 커서이면 빈 결과로 처리된다")
		void searchInvalidCursorReturnsEmpty() {
			SearchResponse response = searchService.search(
				null,
				new SearchRequest("맛집", null, null, null, "invalid-cursor", 10));

			assertThat(response.groups()).isEmpty();
			assertThat(response.restaurants().items()).isEmpty();
		}

		@Test
		@DisplayName("키워드에 공격 문자열이 포함되면 예외가 발생한다")
		void searchUnsafeKeywordFails() {
			assertThatThrownBy(() -> searchService.search(
				null,
				new SearchRequest("<script>alert('hacked')</script>", null, null, null, null, 10)))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(SearchErrorCode.INVALID_SEARCH_KEYWORD.name());
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
