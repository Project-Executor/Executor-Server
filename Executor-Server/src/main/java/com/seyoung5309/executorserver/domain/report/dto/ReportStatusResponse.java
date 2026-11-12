package com.seyoung5309.executorserver.domain.report.dto;

import com.seyoung5309.executorserver.domain.report.entity.Report;
import com.seyoung5309.executorserver.domain.report.entity.ReportStatus;

import java.time.Instant;

/** 신고를 접수하거나 상태를 바꾼 결과. handledAt은 DONE일 때만 값이 있다 */
public record ReportStatusResponse(Long reportId, ReportStatus status, Instant handledAt) {

    public static ReportStatusResponse from(Report report) {
        return new ReportStatusResponse(report.getId(), report.getStatus(), report.getHandledAt());
    }
}
