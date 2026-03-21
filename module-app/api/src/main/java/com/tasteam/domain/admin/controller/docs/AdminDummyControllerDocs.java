package com.tasteam.domain.admin.controller.docs;

import com.tasteam.domain.admin.dto.request.AdminDummySeedRequest;
import com.tasteam.domain.admin.dto.response.AdminDataCountResponse;
import com.tasteam.domain.admin.dto.response.DummySeedStatusResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(170)
@Tag(name = "Admin - Dummy", description = "어드민 더미 데이터 관리 API (부하테스트용)")
public interface AdminDummyControllerDocs {

	@Operation(summary = "더미 데이터 삽입 시작", description = "비동기로 더미 데이터 삽입 작업을 시작합니다. 이미 진행 중이면 409를 반환합니다.")
	void startSeed(AdminDummySeedRequest request);

	@Operation(summary = "시딩 강제 종료", description = "진행 중인 시딩 작업을 현재 스텝 완료 후 중단합니다. 진행 중이 아니면 409를 반환합니다.")
	void cancelSeed();

	@Operation(summary = "시딩 진행 상태 조회", description = "현재 시딩 작업의 진행 상태(IDLE/RUNNING/COMPLETED/FAILED/CANCELLED)와 단계별 진행도를 반환합니다.")
	SuccessResponse<DummySeedStatusResponse> getSeedStatus();

	@Operation(summary = "현재 데이터 개수 조회", description = "더미 데이터가 포함된 주요 테이블의 총 레코드 수를 반환합니다.")
	SuccessResponse<AdminDataCountResponse> count();

	@Operation(summary = "더미 데이터 전체 삭제", description = "email '%@dummy.tasteam.kr', name '더미그룹-%', '더미식당-%' 패턴으로 식별된 더미 데이터를 삭제합니다.")
	void deleteDummyData();
}
