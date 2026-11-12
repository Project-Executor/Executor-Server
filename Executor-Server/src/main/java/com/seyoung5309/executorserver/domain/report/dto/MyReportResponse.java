package com.seyoung5309.executorserver.domain.report.dto;

import com.seyoung5309.executorserver.domain.report.entity.Report;
import com.seyoung5309.executorserver.domain.report.entity.ReportStatus;

import java.time.Instant;

/** 내가 한 신고 한 건. category는 카테고리 이름 */
public record MyReportResponse(Long reportId, String category, String title, ReportStatus status,
                               Instant createdAt, Instant handledAt) {

    public static MyReportResponse from(Report report) {
        return new MyReportResponse(report.getId(), report.getCategory().getName(), report.getTitle(),
                report.getStatus(), report.getCreatedAt(), report.getHandledAt());
    }
}
