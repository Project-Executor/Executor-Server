package com.seyoung5309.executorserver.domain.report.controller;

import com.seyoung5309.executorserver.domain.report.dto.CreateReportRequest;
import com.seyoung5309.executorserver.domain.report.dto.MyReportResponse;
import com.seyoung5309.executorserver.domain.report.dto.ReportCategoryResponse;
import com.seyoung5309.executorserver.domain.report.dto.ReportStatusResponse;
import com.seyoung5309.executorserver.domain.report.service.ReportService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "신고", description = "사용자·스테이지 신고, 버그 제보")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 카테고리 목록", description = "targetRequired가 false인 카테고리(버그 제보, 기타)는 대상 없이 신고할 수 있다.")
    @GetMapping("/report-categories")
    public List<ReportCategoryResponse> getCategories() {
        return reportService.getCategories();
    }

    @Operation(summary = "신고하기",
            description = "대상은 targetUserId와 targetStageId 중 하나만. targetRequired 카테고리는 대상이 꼭 있어야 한다. "
                    + "자기 자신은 신고할 수 없다. title 1~255자, comment 1~2000자.")
    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportStatusResponse create(@LoginUser Long userId, @Valid @RequestBody CreateReportRequest request) {
        return reportService.create(userId, request);
    }

    @Operation(summary = "내 신고 목록", description = "최신순(size 1~50, 기본 20). 처리 상태와 처리 시각 포함.")
    @GetMapping("/reports/me")
    public PageResponse<MyReportResponse> getMyReports(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return reportService.getMyReports(userId, page, size);
    }
}
