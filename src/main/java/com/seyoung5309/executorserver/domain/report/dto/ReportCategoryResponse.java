package com.seyoung5309.executorserver.domain.report.dto;

import com.seyoung5309.executorserver.domain.report.entity.ReportCategory;

/** @param targetRequired 신고 대상(사용자 또는 스테이지)을 꼭 지정해야 하는지. 버그 제보·기타는 false */
public record ReportCategoryResponse(Long categoryId, String name, boolean targetRequired) {

    public static ReportCategoryResponse of(ReportCategory category, boolean targetRequired) {
        return new ReportCategoryResponse(category.getId(), category.getName(), targetRequired);
    }
}
