package com.seyoung5309.executorserver.domain.report.dto;

import com.seyoung5309.executorserver.domain.report.entity.ReportStatus;
import com.seyoung5309.executorserver.global.response.UserSummary;

import java.time.Instant;

/**
 * 관리자용 신고 한 건.
 *
 * @param reporter    신고자. 탈퇴했으면 null
 * @param targetUser  신고 대상 사용자. 없거나 탈퇴했으면 null
 * @param targetStage 신고 대상 스테이지. 없거나 삭제됐으면 null
 */
public record AdminReportResponse(
        Long reportId,
        Category category,
        UserSummary reporter,
        UserSummary targetUser,
        TargetStage targetStage,
        String title,
        String comment,
        ReportStatus status,
        Instant createdAt,
        Instant handledAt
) {
    public record Category(Long categoryId, String name) {
    }

    /** @param official 공식 스테이지면 true, 사용자 맵이면 false */
    public record TargetStage(Long stageId, String title, boolean official) {
    }
}
