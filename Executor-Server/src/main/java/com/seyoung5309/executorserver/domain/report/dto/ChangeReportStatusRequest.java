package com.seyoung5309.executorserver.domain.report.dto;

import com.seyoung5309.executorserver.domain.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeReportStatusRequest(
        @NotNull(message = "status를 입력해 주세요. (RECEIVED, IN_PROGRESS, DONE)")
        ReportStatus status
) {
}
