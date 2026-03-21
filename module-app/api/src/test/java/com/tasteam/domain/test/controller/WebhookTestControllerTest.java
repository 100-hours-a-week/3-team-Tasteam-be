package com.tasteam.domain.test.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.BaseControllerWebMvcTest;
import com.tasteam.domain.member.dto.response.MemberGroupSummaryResponse;
import com.tasteam.domain.member.dto.response.MemberSubgroupSummaryResponse;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.fixture.MemberFixture;

@DisplayName("[유닛](Test) WebhookTestController 단위 테스트")
class WebhookTestControllerTest extends BaseControllerWebMvcTest {

	@Nested
	@DisplayName("에러 테스트")
	class ErrorEndpoints {

		@Test
		@DisplayName("BusinessException 엔드포인트가 500이 아닌 오류 코드를 반환한다")
		void businessException_테스트() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/test/error/business"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		}

		@Test
		@DisplayName("시스템 예외 엔드포인트가 500을 반환한다")
		void systemException_테스트() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/test/error/system"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}

	@Nested
	@DisplayName("개발용 고정 멤버 조회")
	class GetDevMember {

		@Test
		@DisplayName("DEV_MEMBER_ID를 기준으로 멤버 정보를 조회하고 토큰을 반환한다")
		void devMember_조회_성공() throws Exception {
			// given
			Member member = MemberFixture.createWithId(1001L, "dev@example.com", "개발자");
			MemberGroupSummaryResponse groupSummary = new MemberGroupSummaryResponse(
				1L,
				"테스트그룹",
				List.of(new MemberSubgroupSummaryResponse(11L, "테스트하위그룹")));
			given(memberRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.of(member));
			given(memberService.getMyGroupSummaries(1001L)).willReturn(List.of(groupSummary));
			given(jwtTokenProvider.generateAccessToken(1001L, member.getRole().name(), 3_600_000L))
				.willReturn("access-token");
			given(jwtTokenProvider.generateRefreshToken(1001L)).willReturn("refresh-token");
			given(jwtTokenProvider.getExpiration("refresh-token"))
				.willReturn(new Date());

			// when & then
			mockMvc.perform(get("/api/v1/test/dev/member"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberId").value(1001))
				.andExpect(jsonPath("$.data.email").value("dev@example.com"))
				.andExpect(jsonPath("$.data.accessToken").value("access-token"));
		}

		@Test
		@DisplayName("고정 멤버가 없으면 404으로 실패한다")
		void devMember_조회_실패_없음() throws Exception {
			// given
			given(memberRepository.findByIdAndDeletedAtIsNull(1001L)).willReturn(Optional.empty());

			// when & then
			mockMvc.perform(get("/api/v1/test/dev/member"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
		}
	}
}
