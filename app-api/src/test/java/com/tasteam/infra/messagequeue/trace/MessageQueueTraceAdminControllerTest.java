package com.tasteam.infra.messagequeue.trace;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tasteam.config.annotation.ControllerWebMvcTest;

@ControllerWebMvcTest(MessageQueueTraceAdminController.class)
@DisplayName("[유닛](MQ) MessageQueueTraceAdminController 단위 테스트")
class MessageQueueTraceAdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MessageQueueTraceService traceService;

	@Nested
	@DisplayName("MQ 트레이스 조회")
	class FindRecent {

		@Test
		@DisplayName("메시지 ID 필터가 없으면 조회 결과를 반환한다")
		void 트레이스_조회_성공() throws Exception {
			// given
			var log = MessageQueueTraceLog.publish("msg-1", "topic-a", "kafka", "key-1");
			given(traceService.findRecent(any(), anyInt())).willReturn(List.of(log));

			// when & then
			mockMvc.perform(get("/api/v1/admin/mq-traces").param("limit", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].messageId").value("msg-1"))
				.andExpect(jsonPath("$.data[0].topic").value("topic-a"))
				.andExpect(jsonPath("$.data[0].stage").value("PUBLISH"));
		}

		@Test
		@DisplayName("messageId로 필터링해 조회한 결과를 반환한다")
		void 트레이스_조회_메시지필터_성공() throws Exception {
			// given
			var log = MessageQueueTraceLog.consumeSuccess(
				"msg-2",
				"topic-b",
				"kafka",
				"key-2",
				"group-a",
				123);
			given(traceService.findRecent("msg-2", 50)).willReturn(List.of(log));

			// when & then
			mockMvc.perform(get("/api/v1/admin/mq-traces")
				.param("messageId", "msg-2")
				.param("limit", "50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].messageId").value("msg-2"))
				.andExpect(jsonPath("$.data[0].consumerGroup").value("group-a"));
		}

		@Test
		@DisplayName("limit 타입이 잘못되면 400으로 실패한다")
		void 트레이스_조회_제한값_타입실패() throws Exception {
			// when & then
			mockMvc.perform(get("/api/v1/admin/mq-traces").param("limit", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		}

		@Test
		@DisplayName("limit 상한을 넘으면 200으로 결과를 반환하고 상한으로 클램프한다")
		void 트레이스_조회_클램프_적용() throws Exception {
			// given
			var log = MessageQueueTraceLog.consumeFail(
				"msg-3",
				"topic-c",
				"kafka",
				"key-3",
				"group-b",
				50,
				"error");
			given(traceService.findRecent(isNull(), eq(200))).willReturn(List.of(log));

			// when & then
			mockMvc.perform(get("/api/v1/admin/mq-traces").param("limit", "1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

			verify(traceService).findRecent(isNull(), eq(200));
		}

		@Test
		@DisplayName("조회 중 예외가 발생하면 500으로 실패한다")
		void 트레이스_조회_실패() throws Exception {
			// given
			willThrow(new RuntimeException("trace fail")).given(traceService).findRecent(isNull(), anyInt());

			// when & then
			mockMvc.perform(get("/api/v1/admin/mq-traces").param("limit", "20"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.success").value(false));
		}
	}
}
