package com.seyoung5309.executorserver.domain.report.controller;

import com.seyoung5309.executorserver.domain.report.dto.AdminReportResponse;
import com.seyoung5309.executorserver.domain.report.dto.ChangeReportStatusRequest;
import com.seyoung5309.executorserver.domain.report.dto.ReportStatusResponse;
import com.seyoung5309.executorserver.domain.report.service.AdminReportService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 신고", description = "ADMIN 권한 필요")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @Operation(summary = "신고 목록",
            description = "status(RECEIVED, IN_PROGRESS, DONE)를 빼면 전체. 최신순(size 1~50, 기본 20). "
                    + "탈퇴한 신고자·대상이나 삭제된 스테이지는 null.")
    @GetMapping
    public PageResponse<AdminReportResponse> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return adminReportService.getReports(AdminReportService.parseStatus(status), page, size);
    }

    @Operation(summary = "신고 상태 변경",
            description = "앞으로만 진행(RECEIVED -> IN_PROGRESS -> DONE). 되돌리면 400. DONE이 되면 handledAt을 기록한다.")
    @PatchMapping("/{reportId}")
    public ReportStatusResponse changeStatus(@PathVariable Long reportId,
                                             @Valid @RequestBody ChangeReportStatusRequest request) {
        return adminReportService.changeStatus(reportId, request.status());
    }
}
