package com.tasteam.domain.notification.controller.docs;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.tasteam.domain.notification.dto.request.NotificationPreferenceUpdateRequest;
import com.tasteam.domain.notification.dto.request.PushNotificationTargetRegisterRequest;
import com.tasteam.domain.notification.dto.response.NotificationPreferenceResponse;
import com.tasteam.domain.notification.dto.response.NotificationResponse;
import com.tasteam.domain.notification.dto.response.UnreadCountResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.notification.NotificationSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

@Tag(name = "Notification", description = "알림 API")
public interface NotificationControllerDocs {

	@Operation(summary = "알림 목록 조회", description = "현재 로그인 사용자의 알림 목록을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = NotificationResponse.class)))
	@CustomErrorResponseDescription(value = NotificationSwaggerErrorResponseDescription.class, group = "NOTIFICATION_LIST")
	SuccessResponse<OffsetPageResponse<NotificationResponse>> getNotifications(
		@CurrentUser
		Long memberId,
		@Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
		int page,
		@Parameter(description = "페이지 크기 (최대 100)", example = "10") @Max(100)
		int size);

	@Operation(summary = "개별 알림 읽음 처리", description = "특정 알림을 읽음 처리합니다. 이미 읽은 알림도 204를 반환합니다.")
	@ApiResponse(responseCode = "204", description = "처리 완료")
	@CustomErrorResponseDescription(value = NotificationSwaggerErrorResponseDescription.class, group = "NOTIFICATION_READ")
	ResponseEntity<Void> markAsRead(
		@CurrentUser
		Long memberId,
		@Parameter(description = "알림 ID", example = "1") @Positive
		Long id);

	@Operation(summary = "전체 알림 읽음 처리", description = "현재 로그인 사용자의 모든 미읽음 알림을 읽음 처리합니다.")
	@ApiResponse(responseCode = "204", description = "처리 완료")
	@CustomErrorResponseDescription(value = NotificationSwaggerErrorResponseDescription.class, group = "NOTIFICATION_READ_ALL")
	ResponseEntity<Void> markAllAsRead(@CurrentUser
	Long memberId);

	@Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 로그인 사용자의 읽지 않은 알림 개수를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = UnreadCountResponse.class)))
	@CustomErrorResponseDescription(value = NotificationSwaggerErrorResponseDescription.class, group = "NOTIFICATION_UNREAD_COUNT")
	SuccessResponse<UnreadCountResponse> getUnreadCount(@CurrentUser
	Long memberId);

	@Operation(summary = "알림 선호도 목록 조회", description = "현재 로그인 사용자의 알림 수신 선호도를 조회합니다. 모든 채널x유형 조합에 대해 응답합니다.")
	@ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = NotificationPreferenceResponse.class)))
	@CustomErrorResponseDescription(value = NotificationSwaggerErrorResponseDescription.class, group = "NOTIFICATION_PREFERENCE_LIST")
	SuccessResponse<List<NotificationPreferenceResponse>> getPreferences(@CurrentUser
	Long memberId);

	@Operation(summary = "알림 선호도 수정", description = "현재 로그인 사용자의 알림 수신 선호도를 수정합니다. 전달된 항목만 수정됩니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = NotificationPreferenceUpdateRequest.class)))
	@ApiResponse(responseCode = "204", description = "수정 완료")
	@CustomErrorResponseDescription(value = NotificationSwaggerErrorResponseDescription.class, group = "NOTIFICATION_PREFERENCE_UPDATE")
	ResponseEntity<Void> updatePreferences(
		@CurrentUser
		Long memberId,
		@Valid
		NotificationPreferenceUpdateRequest request);

	@Operation(summary = "FCM 토큰 등록", description = "푸시 알림 수신을 위한 FCM 토큰을 등록합니다. 이미 등록된 토큰은 새 사용자로 재등록됩니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = PushNotificationTargetRegisterRequest.class)))
	@ApiResponse(responseCode = "204", description = "등록 완료")
	@CustomErrorResponseDescription(value = NotificationSwaggerErrorResponseDescription.class, group = "PUSH_TARGET_REGISTER")
	ResponseEntity<Void> registerPushTarget(
		@CurrentUser
		Long memberId,
		@Valid
		PushNotificationTargetRegisterRequest request);
}
