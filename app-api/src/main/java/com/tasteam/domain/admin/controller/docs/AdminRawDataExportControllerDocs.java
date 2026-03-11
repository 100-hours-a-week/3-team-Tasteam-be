package com.tasteam.domain.admin.controller.docs;

import com.tasteam.domain.admin.dto.request.AdminRawDataExportRequest;
import com.tasteam.domain.analytics.export.RawDataExportResult;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(166)
@Tag(name = "Admin - Analytics Raw Export", description = "개발/스테이징 raw 데이터 수동 export API")
public interface AdminRawDataExportControllerDocs {

	@Operation(summary = "raw 데이터 수동 export 실행", description = "dev/stg 프로필에서만 raw/restaurants, raw/menus export를 실행합니다.")
	@ApiResponse(responseCode = "200", description = "실행 완료")
	SuccessResponse<RawDataExportResult> runRawExport(AdminRawDataExportRequest request);
}
