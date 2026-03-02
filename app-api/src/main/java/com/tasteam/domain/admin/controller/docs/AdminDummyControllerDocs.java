package com.tasteam.domain.admin.controller.docs;

import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.domain.admin.dto.response.AdminDataCountResponse;
import com.tasteam.domain.admin.dto.response.AdminDummySeedResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(170)
@Tag(name = "Admin - Dummy", description = "어드민 더미 데이터 관리 API (부하테스트용)")
public interface AdminDummyControllerDocs {

	@Operation(summary = "더미 데이터 삽입", description = "지정한 수량만큼 더미 멤버/음식점/그룹/하위그룹/리뷰/채팅 메시지를 JDBC batch INSERT로 삽입합니다.")
	SuccessResponse<AdminDummySeedResponse> seed(AdminDummySeedRequest request);

	@Operation(summary = "현재 데이터 개수 조회", description = "각 테이블의 soft-delete 제외 총 레코드 수를 반환합니다.")
	SuccessResponse<AdminDataCountResponse> count();

	@Operation(summary = "더미 데이터 전체 삭제", description = "email '%@dummy.tasteam.kr', name '더미그룹-%', '더미식당-%' 패턴으로 식별된 더미 데이터를 삭제합니다.")
	void deleteDummyData();
}
